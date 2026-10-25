
-- EcoRuta - Migración v2: Alineación Normativa IEEE 830, Antigravity Rules y Figma
-- Archivo: 20261025063000_v2_changes.sql
-- Dependencia previa: 20261025055033_remote_schema.sql



-- Sub roles de operador ('administrador_empresa', 'despachador', 'chofer')


-- Migrar registros previos con 'conductor' hacia 'chofer'
UPDATE public.operators 
SET operator_type = 'chofer' 
WHERE operator_type = 'conductor';

-- Actualizar restricción CHECK en tabla operators
ALTER TABLE public.operators 
  DROP CONSTRAINT IF EXISTS operators_operator_type_check;

ALTER TABLE public.operators 
  ADD CONSTRAINT operators_operator_type_check 
  CHECK (operator_type IN ('administrador_empresa', 'despachador', 'chofer'));



-- Ciclo de vida de rutas ya asignación
-- Estados: 'borrador', 'publicada', 'en_curso', 'finalizada', 'cancelada'
-- Tipos de ruta: 'ordinaria', 'dinamica', 'reciclaje'


-- Migrar estados previos hacia el estándar canónico
UPDATE public.routes 
SET status = 'en_curso' 
WHERE status = 'activa';

UPDATE public.routes 
SET status = 'finalizada' 
WHERE status = 'completada';

-- Actualizar restricciones CHECK en tabla routes
ALTER TABLE public.routes 
  DROP CONSTRAINT IF EXISTS routes_status_check;

ALTER TABLE public.routes 
  ADD CONSTRAINT routes_status_check 
  CHECK (status IN ('borrador', 'publicada', 'en_curso', 'finalizada', 'cancelada'));

ALTER TABLE public.routes 
  DROP CONSTRAINT IF EXISTS routes_route_type_check;

ALTER TABLE public.routes 
  ADD CONSTRAINT routes_route_type_check 
  CHECK (route_type IN ('ordinaria', 'dinamica', 'reciclaje'));

-- Agregar campos de asignación y auditoría temporal a routes
ALTER TABLE public.routes 
  ADD COLUMN IF NOT EXISTS driver_id uuid REFERENCES public.operators(profile_id),
  ADD COLUMN IF NOT EXISTS recycler_id uuid REFERENCES public.recyclers(profile_id),
  ADD COLUMN IF NOT EXISTS published_at timestamptz,
  ADD COLUMN IF NOT EXISTS started_at timestamptz,
  ADD COLUMN IF NOT EXISTS completed_at timestamptz;

CREATE INDEX IF NOT EXISTS routes_driver_id_idx ON public.routes(driver_id);
CREATE INDEX IF NOT EXISTS routes_recycler_id_idx ON public.routes(recycler_id);

-- Asociar recicladores a empresas o asociaciones de reciclaje (opcional)
ALTER TABLE public.recyclers
  ADD COLUMN IF NOT EXISTS company_id uuid REFERENCES public.companies(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS recyclers_company_id_idx ON public.recyclers(company_id);



-- Pesos de priorización normativos IEEE380 
-- w1: Antigüedad (0.30), w2: Reincidencia (0.30), w3: Tipo afectación (0.25), w4: Densidad zonal (0.15)


ALTER TABLE public.priority_weights 
  ALTER COLUMN w1 SET DEFAULT 0.30,
  ALTER COLUMN w2 SET DEFAULT 0.30,
  ALTER COLUMN w3 SET DEFAULT 0.25,
  ALTER COLUMN w4 SET DEFAULT 0.15;

-- Actualizar registros existentes que conserven los valores obsoletos (0.4, 0.3, 0.2, 0.1)
UPDATE public.priority_weights
SET w1 = 0.30, w2 = 0.30, w3 = 0.25, w4 = 0.15
WHERE w1 = 0.40 AND w2 = 0.30 AND w3 = 0.20 AND w4 = 0.10;

-- Actualizar RPC get_ranked_collection_points con fallback normativo y search_path seguro
CREATE OR REPLACE FUNCTION public.get_ranked_collection_points (
  p_origin_location public.geography DEFAULT NULL::public.geography
)
  RETURNS TABLE (
    point_id               uuid,
    label                  text,
    neighborhood           text,
    location               public.geography,
    approved_reports_count bigint,
    zone_density           bigint,
    oldest_report_days     numeric,
    distance_km            numeric,
    priority_score         numeric
  )
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path = public, auth
  AS $function$
DECLARE
  v_company_id uuid;
  v_w1 numeric;
  v_w2 numeric;
  v_w3 numeric;
  v_w4 numeric;
BEGIN
  -- A. Identificar la empresa del operador que consulta
  SELECT company_id INTO v_company_id
  FROM operators
  WHERE profile_id = auth.uid();

  -- B. Obtener pesos configurados por la empresa
  SELECT w1, w2, w3, w4 INTO v_w1, v_w2, v_w3, v_w4
  FROM priority_weights
  WHERE company_id = v_company_id;

  -- Fallback normativo según IEEE 830: 30%, 30%, 25%, 15%
  IF NOT FOUND THEN
    v_w1 := 0.30; v_w2 := 0.30; v_w3 := 0.25; v_w4 := 0.15;
  END IF;

  -- C. Calcular puntaje ponderado de urgencia
  RETURN QUERY
  SELECT 
    v.point_id,
    v.label,
    v.neighborhood,
    v.location,
    v.approved_reports_count,
    v.zone_density,
    v.oldest_report_days,
    
    CASE 
      WHEN p_origin_location IS NOT NULL THEN (ST_Distance(v.location, p_origin_location) / 1000.0)::numeric
      ELSE 0
    END AS distance_km,
    
    (
      (v_w1 * v.approved_reports_count) + 
      (v_w2 * v.zone_density) + 
      (v_w3 * v.oldest_report_days) - 
      (v_w4 * (
        CASE 
          WHEN p_origin_location IS NOT NULL THEN (ST_Distance(v.location, p_origin_location) / 1000.0)
          ELSE 0
        END
      ))
    )::numeric AS priority_score

  FROM collection_points_priority_view v
  WHERE v.approved_reports_count > 0
  ORDER BY priority_score DESC;
END;
$function$;

-- Fotografías de reportes (Hasta 3 fotos con validación)

CREATE TABLE IF NOT EXISTS public.report_photos (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id uuid NOT NULL REFERENCES public.reports(id) ON DELETE CASCADE,
    photo_url text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS report_photos_report_id_idx ON public.report_photos(report_id);

CREATE OR REPLACE FUNCTION public.check_max_report_photos()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $function$
BEGIN
  IF (SELECT count(*) FROM public.report_photos WHERE report_id = NEW.report_id) >= 3 THEN
    RAISE EXCEPTION 'Un reporte no puede contener más de 3 fotografías de evidencia.';
  END IF;
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_check_max_report_photos ON public.report_photos;
CREATE TRIGGER trg_check_max_report_photos
  BEFORE INSERT ON public.report_photos
  FOR EACH ROW
  EXECUTE FUNCTION public.check_max_report_photos();

-- Migrar la foto principal existente en reports a report_photos si aún no existe
INSERT INTO public.report_photos (report_id, photo_url)
SELECT id, photo_url 
FROM public.reports 
WHERE photo_url IS NOT NULL 
  AND NOT EXISTS (
    SELECT 1 FROM public.report_photos rp WHERE rp.report_id = reports.id
  );

-- 5. CICLO DE VIDA DE CUPONES Y VALIDACIÓN EN COMERCIO (Figma / Negocios)
ALTER TABLE public.coupon_redemptions
  ADD COLUMN IF NOT EXISTS code text,
  ADD COLUMN IF NOT EXISTS status text NOT NULL DEFAULT 'emitido' CHECK (status IN ('emitido', 'canjeado', 'expirado')),
  ADD COLUMN IF NOT EXISTS used_at timestamptz,
  ADD COLUMN IF NOT EXISTS validated_by uuid REFERENCES public.businesses(profile_id);

CREATE UNIQUE INDEX IF NOT EXISTS coupon_redemptions_code_key 
  ON public.coupon_redemptions(code) 
  WHERE code IS NOT NULL;

-- Generar códigos para redenciones previas que no lo tengan
UPDATE public.coupon_redemptions
SET code = 'ECO-' || upper(substr(md5(id::text || clock_timestamp()::text), 1, 8))
WHERE code IS NULL;

-- Actualizar RPC redeem_coupon para generar código de canje alfanumérico
CREATE OR REPLACE FUNCTION public.redeem_coupon (
  p_coupon_id uuid
)
  RETURNS json
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path = public, auth
  AS $function$
DECLARE
  v_citizen_id uuid := auth.uid();
  v_coupon record;
  v_balance int;
  v_redemption_id uuid;
  v_code text;
BEGIN
  IF v_citizen_id IS NULL THEN
    RAISE EXCEPTION 'Usuario no autenticado.';
  END IF;

  -- A. Consultar cupón con bloqueo de fila para evitar sobreventas concurrentes
  SELECT * INTO v_coupon FROM coupons WHERE id = p_coupon_id FOR UPDATE;
  IF NOT FOUND OR NOT v_coupon.is_active THEN
    RAISE EXCEPTION 'El cupón no está disponible o no existe.';
  END IF;

  IF v_coupon.quantity_available <= 0 THEN
    RAISE EXCEPTION 'El cupón se ha agotado.';
  END IF;

  IF v_coupon.valid_until IS NOT NULL AND v_coupon.valid_until < CURRENT_DATE THEN
    RAISE EXCEPTION 'El cupón ha expirado.';
  END IF;

  -- B. Verificar saldo suficiente de EcoPuntos
  SELECT ecopoints_balance INTO v_balance FROM citizens WHERE profile_id = v_citizen_id;
  IF v_balance IS NULL OR v_balance < v_coupon.points_cost THEN
    RAISE EXCEPTION 'Saldo insuficiente de EcoPuntos.';
  END IF;

  -- C. Reducir stock del cupón
  UPDATE coupons 
  SET quantity_available = quantity_available - 1 
  WHERE id = p_coupon_id;

  -- D. Generar código legible único (ECO-XXXXXXXX)
  v_code := 'ECO-' || upper(substr(md5(random()::text || clock_timestamp()::text), 1, 8));

  -- E. Registrar la redención
  INSERT INTO coupon_redemptions (citizen_id, coupon_id, code, status)
  VALUES (v_citizen_id, p_coupon_id, v_code, 'emitido')
  RETURNING id INTO v_redemption_id;

  -- F. Registrar transacción negativa (El trigger sync_citizen_ecopoints_balance deduce el saldo)
  INSERT INTO ecopoints_transactions (citizen_id, points, reason_code, redemption_id)
  VALUES (v_citizen_id, -abs(v_coupon.points_cost), 'redencion_cupon', v_redemption_id);

  RETURN json_build_object(
    'success', true,
    'redemption_id', v_redemption_id,
    'code', v_code,
    'message', 'Cupón redimido exitosamente. Presenta este código en el comercio.'
  );
END;
$function$;

-- RPC para que los comercios validen y reclamen el cupón de un ciudadano
CREATE OR REPLACE FUNCTION public.validate_coupon_redemption(
  p_code text
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $function$
DECLARE
  v_business_id uuid := auth.uid();
  v_redemption record;
  v_coupon record;
BEGIN
  IF v_business_id IS NULL THEN
    RAISE EXCEPTION 'Debe iniciar sesión como comercio para validar cupones.';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM businesses WHERE profile_id = v_business_id) THEN
    RAISE EXCEPTION 'Solo los comercios autorizados pueden validar cupones.';
  END IF;

  SELECT * INTO v_redemption 
  FROM coupon_redemptions 
  WHERE code = upper(trim(p_code))
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Código de cupón no encontrado o inválido.';
  END IF;

  IF v_redemption.status = 'canjeado' THEN
    RAISE EXCEPTION 'Este cupón ya fue canjeado el %.', to_char(v_redemption.used_at, 'DD/MM/YYYY HH24:MI');
  END IF;

  IF v_redemption.status = 'expirado' THEN
    RAISE EXCEPTION 'Este cupón ha expirado.';
  END IF;

  SELECT * INTO v_coupon FROM coupons WHERE id = v_redemption.coupon_id;
  IF v_coupon.business_id <> v_business_id THEN
    RAISE EXCEPTION 'Este cupón pertenece a otro establecimiento comercial.';
  END IF;

  UPDATE coupon_redemptions
  SET status = 'canjeado',
      used_at = now(),
      validated_by = v_business_id
  WHERE id = v_redemption.id;

  RETURN json_build_object(
    'success', true,
    'coupon_title', v_coupon.title,
    'redemption_id', v_redemption.id,
    'used_at', now(),
    'message', 'Cupón validado y canjeado exitosamente.'
  );
END;
$function$;



-- 6. RADAR DE ALERTAS CIUDADANAS (Desacoplado de la tabla citizens)


CREATE TABLE IF NOT EXISTS public.radar_subscriptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    citizen_id uuid NOT NULL REFERENCES public.citizens(profile_id) ON DELETE CASCADE,
    name text NOT NULL DEFAULT 'Mi Hogar',
    center_location geography(Point, 4326) NOT NULL,
    radius_meters int NOT NULL DEFAULT 300 CHECK (radius_meters BETWEEN 50 AND 2000),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS radar_subscriptions_citizen_id_idx ON public.radar_subscriptions(citizen_id);
CREATE INDEX IF NOT EXISTS radar_subscriptions_center_location_idx ON public.radar_subscriptions USING gist(center_location);

DROP TRIGGER IF EXISTS update_radar_subscriptions_updated_at ON public.radar_subscriptions;
CREATE TRIGGER update_radar_subscriptions_updated_at
  BEFORE UPDATE ON public.radar_subscriptions
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

-- Migrar datos iniciales de citizens.home_location si existieran
INSERT INTO public.radar_subscriptions (citizen_id, name, center_location, radius_meters, is_active)
SELECT 
    c.profile_id,
    'Mi Hogar',
    c.home_location,
    COALESCE(c.notification_radius_meters, 300),
    true
FROM public.citizens c
WHERE c.home_location IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM public.radar_subscriptions rs WHERE rs.citizen_id = c.profile_id
  );



-- 7. SOPORTE MULTI-ROL Y CAMBIO DINÁMICO DE ROL ACTIVO


ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS active_role text REFERENCES public.roles(code);

UPDATE public.profiles
SET active_role = role_code
WHERE active_role IS NULL;

CREATE TABLE IF NOT EXISTS public.user_roles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    role_code text NOT NULL REFERENCES public.roles(code) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_roles_profile_id_role_code_key UNIQUE (profile_id, role_code)
);

CREATE INDEX IF NOT EXISTS user_roles_profile_id_idx ON public.user_roles(profile_id);

-- Sincronizar roles existentes de profiles hacia user_roles
INSERT INTO public.user_roles (profile_id, role_code)
SELECT id, role_code 
FROM public.profiles
ON CONFLICT (profile_id, role_code) DO NOTHING;

-- RPC para listar roles que posee el usuario autenticado
CREATE OR REPLACE FUNCTION public.get_my_roles()
RETURNS TABLE (role_code text, label text, description text, is_active boolean)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $function$
DECLARE
  v_user_id uuid := auth.uid();
  v_active_role text;
BEGIN
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'Usuario no autenticado.';
  END IF;

  SELECT active_role INTO v_active_role FROM profiles WHERE id = v_user_id;

  RETURN QUERY
  SELECT 
    r.code AS role_code,
    r.label,
    r.description,
    (r.code = v_active_role) AS is_active
  FROM user_roles ur
  JOIN roles r ON r.code = ur.role_code
  WHERE ur.profile_id = v_user_id;
END;
$function$;

-- RPC para alternar el rol activo en sesión
CREATE OR REPLACE FUNCTION public.switch_active_role(p_role_code text)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $function$
DECLARE
  v_user_id uuid := auth.uid();
BEGIN
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'Usuario no autenticado.';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM user_roles WHERE profile_id = v_user_id AND role_code = p_role_code) THEN
    RAISE EXCEPTION 'El usuario no tiene asignado el rol %.', p_role_code;
  END IF;

  UPDATE profiles
  SET active_role = p_role_code,
      role_code = p_role_code
  WHERE id = v_user_id;

  RETURN json_build_object(
    'success', true,
    'active_role', p_role_code,
    'message', 'Rol activo actualizado exitosamente.'
  );
END;
$function$;



--NOTIFICACIONES PUSH Y DISPOSITIVOS MÓVILES (FCM)


CREATE TABLE IF NOT EXISTS public.user_fcm_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    token text NOT NULL,
    device_model text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_fcm_tokens_profile_token_key UNIQUE (profile_id, token)
);

CREATE INDEX IF NOT EXISTS user_fcm_tokens_profile_id_idx ON public.user_fcm_tokens(profile_id);

DROP TRIGGER IF EXISTS update_user_fcm_tokens_updated_at ON public.user_fcm_tokens;
CREATE TRIGGER update_user_fcm_tokens_updated_at
  BEFORE UPDATE ON public.user_fcm_tokens
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TABLE IF NOT EXISTS public.notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title text NOT NULL,
    body text NOT NULL,
    type text NOT NULL CHECK (type IN ('radar_alerta', 'reporte_actualizado', 'cupon_canjeado', 'sistema')),
    data jsonb,
    is_read boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS notifications_profile_id_idx ON public.notifications(profile_id);
CREATE INDEX IF NOT EXISTS notifications_is_read_idx ON public.notifications(is_read);



-- PREVENCIÓN DE DOBLE ACREDITACIÓN (Índices únicos parciales)


CREATE UNIQUE INDEX IF NOT EXISTS ecopoints_transactions_unique_pickup 
  ON public.ecopoints_transactions(pickup_id) 
  WHERE pickup_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ecopoints_transactions_unique_redemption 
  ON public.ecopoints_transactions(redemption_id) 
  WHERE redemption_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ecopoints_transactions_unique_completion 
  ON public.ecopoints_transactions(citizen_id, completion_id) 
  WHERE completion_id IS NOT NULL;



-- POLÍTICAS DE SEGURIDAD RLS (Modo Visitante y Tablas Nuevas)

-- Rutas: Permitir a visitantes y ciudadanos leer rutas publicadas, en curso y finalizadas
DROP POLICY IF EXISTS "Cualquiera lee rutas activas" ON public.routes;
DROP POLICY IF EXISTS "Lectura publica de rutas publicadas y en curso" ON public.routes;

CREATE POLICY "Lectura publica de rutas publicadas y en curso" ON public.routes
  FOR SELECT
  TO PUBLIC
  USING (
    status IN ('publicada', 'en_curso', 'finalizada') 
    OR (EXISTS (
      SELECT 1 FROM public.profiles 
      WHERE profiles.id = auth.uid() 
        AND profiles.role_code = 'operator'
    ))
  );

-- Puntos Limpios: Modo visitante (lectura libre de puntos de recolección)
ALTER TABLE public.collection_points ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Lectura publica de puntos limpios" ON public.collection_points;
CREATE POLICY "Lectura publica de puntos limpios" ON public.collection_points
  FOR SELECT
  TO PUBLIC
  USING (true);

-- Fotografías de reportes
ALTER TABLE public.report_photos ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Lectura publica de fotos de reportes" ON public.report_photos;
CREATE POLICY "Lectura publica de fotos de reportes" ON public.report_photos
  FOR SELECT TO PUBLIC USING (true);

DROP POLICY IF EXISTS "Ciudadanos agregan fotos a sus reportes" ON public.report_photos;
CREATE POLICY "Ciudadanos agregan fotos a sus reportes" ON public.report_photos
  FOR INSERT TO PUBLIC WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.reports 
      WHERE reports.id = report_photos.report_id 
        AND reports.citizen_id = auth.uid()
    )
  );

--  Radar de Alertas
ALTER TABLE public.radar_subscriptions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ciudadanos gestionan su propio radar" ON public.radar_subscriptions;
CREATE POLICY "Ciudadanos gestionan su propio radar" ON public.radar_subscriptions
  FOR ALL TO PUBLIC
  USING (citizen_id = auth.uid())
  WITH CHECK (citizen_id = auth.uid());

-- Tokens FCM
ALTER TABLE public.user_fcm_tokens ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Usuarios gestionan sus tokens fcm" ON public.user_fcm_tokens;
CREATE POLICY "Usuarios gestionan sus tokens fcm" ON public.user_fcm_tokens
  FOR ALL TO PUBLIC
  USING (profile_id = auth.uid())
  WITH CHECK (profile_id = auth.uid());

-- Notificaciones
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Usuarios leen sus notificaciones" ON public.notifications;
CREATE POLICY "Usuarios leen sus notificaciones" ON public.notifications
  FOR SELECT TO PUBLIC
  USING (profile_id = auth.uid());

DROP POLICY IF EXISTS "Usuarios marcan leidas sus notificaciones" ON public.notifications;
CREATE POLICY "Usuarios marcan leidas sus notificaciones" ON public.notifications
  FOR UPDATE TO PUBLIC
  USING (profile_id = auth.uid())
  WITH CHECK (profile_id = auth.uid());

--Multi-rol
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Usuarios consultan sus roles asignados" ON public.user_roles;
CREATE POLICY "Usuarios consultan sus roles asignados" ON public.user_roles
  FOR SELECT TO PUBLIC
  USING (profile_id = auth.uid());

-- Inserción de perfiles tras registro de usuario en Auth
DROP POLICY IF EXISTS "Los usuarios insertan su propio perfil" ON public.profiles;
CREATE POLICY "Los usuarios insertan su propio perfil" ON public.profiles
  FOR INSERT TO PUBLIC
  WITH CHECK (auth.uid() = id);

--CONCESIÓN DE PERMISOS (GRANTS para API REST / Supabase PostgREST)


GRANT SELECT ON TABLE public.collection_points TO anon, authenticated;
GRANT SELECT ON TABLE public.routes TO anon, authenticated;
GRANT SELECT ON TABLE public.educational_content TO anon, authenticated;

GRANT ALL ON TABLE public.report_photos TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.radar_subscriptions TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.user_fcm_tokens TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.notifications TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.user_roles TO anon, authenticated, service_role;

GRANT EXECUTE ON FUNCTION public.get_ranked_collection_points(public.geography) TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.redeem_coupon(uuid) TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.validate_coupon_redemption(text) TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.get_my_roles() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.switch_active_role(text) TO authenticated, service_role;
