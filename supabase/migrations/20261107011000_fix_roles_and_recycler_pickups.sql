-- Migración de corrección para catálogos base, consulta de roles y ciclo de reserva o cancelación de reciclaje
-- Garantiza la integridad de llaves foráneas en local, corrige get_my_roles y habilita la gestión de órdenes de recolección

-- Asegura que el catálogo de roles exista antes de cualquier registro de usuario
INSERT INTO public.roles (code, name, description, is_active)
VALUES
  (
    'citizen',
    'Ciudadano',
    'Ciudadano registrado en el municipio de Tuluá para reportes, reciclaje, educación y EcoPuntos',
    true
  ),
  (
    'recycler',
    'Reciclador de Oficio',
    'Reciclador independiente o vinculado a asociación/empresa para recolección de material aprovechable',
    true
  ),
  (
    'business',
    'Comercio Aliado',
    'Establecimiento comercial aliado que publica beneficios y valida cupones por EcoPuntos',
    true
  ),
  (
    'operator',
    'Operador',
    'Personal operativo de empresa gestora (administrador de empresa, despachador o chofer)',
    true
  ),
  (
    'company_admin',
    'Administrador de Empresa',
    'Administrador institucional de empresa gestora (aseo, reciclaje y/o escombros): administra equipo operativo, vinculación de recicladores y parámetros',
    true
  ),
  (
    'admin',
    'Administrador EcoRuta',
    'Superadministrador web: gestiona empresas prestadoras, verifica comercios y recicladores, y supervisa catálogos',
    true
  )
ON CONFLICT (code) DO UPDATE
SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  is_active = EXCLUDED.is_active;

-- Asegura que los motivos de transacción existan para la acreditación y canje de EcoPuntos
INSERT INTO public.transaction_reasons (code, name, type, default_points, is_active)
VALUES
  (
    'entrega_material',
    'Entrega de material reciclable',
    'earn',
    10,
    true
  ),
  (
    'capacitacion',
    'Capacitación ambiental completada',
    'earn',
    20,
    true
  ),
  (
    'redencion_cupon',
    'Redención de cupón en comercio aliado',
    'spend',
    0,
    true
  )
ON CONFLICT (code) DO UPDATE
SET
  name = EXCLUDED.name,
  type = EXCLUDED.type,
  default_points = EXCLUDED.default_points,
  is_active = EXCLUDED.is_active;

-- Corrige la consulta de roles asignados proyectando la columna name de la tabla roles como label
CREATE OR REPLACE FUNCTION public.get_my_roles()
RETURNS TABLE (
  role_code   text,
  label       text,
  description text,
  is_active   boolean
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'auth'
AS $function$
DECLARE
  v_user_id uuid := auth.uid();
  v_active_role text;
BEGIN
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'Usuario no autenticado.';
  END IF;

  SELECT COALESCE(p.active_role, p.role_code) INTO v_active_role FROM public.profiles p WHERE p.id = v_user_id;

  RETURN QUERY
  SELECT 
    r.code AS role_code,
    r.name AS label,
    r.description,
    (r.code = v_active_role) AS is_active
  FROM public.user_roles ur
  JOIN public.roles r ON r.code = ur.role_code
  WHERE ur.profile_id = v_user_id;
END;
$function$;

-- Inicializa active_role por defecto al crear un perfil nuevo si no fue enviado explícitamente
CREATE OR REPLACE FUNCTION public.set_default_active_role_on_profile()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
BEGIN
  IF NEW.active_role IS NULL THEN
    NEW.active_role := NEW.role_code;
  END IF;
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_set_default_active_role_on_profile ON public.profiles;
CREATE TRIGGER trg_set_default_active_role_on_profile
  BEFORE INSERT ON public.profiles
  FOR EACH ROW
  EXECUTE FUNCTION public.set_default_active_role_on_profile();

-- Registra automáticamente el rol inicial y el rol ciudadano en user_roles cuando se crea un perfil
CREATE OR REPLACE FUNCTION public.sync_user_roles_on_profile_insert()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
BEGIN
  INSERT INTO public.user_roles (profile_id, role_code)
  VALUES (NEW.id, NEW.role_code)
  ON CONFLICT (profile_id, role_code) DO NOTHING;

  INSERT INTO public.user_roles (profile_id, role_code)
  VALUES (NEW.id, 'citizen')
  ON CONFLICT (profile_id, role_code) DO NOTHING;

  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_sync_user_roles_on_profile_insert ON public.profiles;
CREATE TRIGGER trg_sync_user_roles_on_profile_insert
  AFTER INSERT ON public.profiles
  FOR EACH ROW
  EXECUTE FUNCTION public.sync_user_roles_on_profile_insert();

-- Sincroniza perfiles existentes que aún no tengan active_role o su fila correspondiente en user_roles
UPDATE public.profiles
SET active_role = role_code
WHERE active_role IS NULL;

INSERT INTO public.user_roles (profile_id, role_code)
SELECT id, role_code
FROM public.profiles
ON CONFLICT (profile_id, role_code) DO NOTHING;

-- Agrega campos de dirección, barrio, evidencia y fecha de cancelación a las publicaciones de material
ALTER TABLE public.recyclable_material_listings
  ADD COLUMN IF NOT EXISTS address text,
  ADD COLUMN IF NOT EXISTS neighborhood text,
  ADD COLUMN IF NOT EXISTS photo_url text,
  ADD COLUMN IF NOT EXISTS cancelled_at timestamptz;

-- Agrega fecha de cancelación a las órdenes de recolección de recicladores
ALTER TABLE public.recycler_pickups
  ADD COLUMN IF NOT EXISTS cancelled_at timestamptz;

-- Amplía los tipos de notificación permitidos para cubrir avisos de reserva y cancelación de recolección
ALTER TABLE public.notifications
  DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE public.notifications
  ADD CONSTRAINT notifications_type_check
  CHECK (type IN (
    'radar_alerta',
    'reporte_actualizado',
    'cupon_canjeado',
    'recoleccion_reservada',
    'recoleccion_cancelada',
    'sistema'
  ));

-- Registra automáticamente la marca de tiempo cuando una publicación cambia a estado cancelado
CREATE OR REPLACE FUNCTION public.set_listing_cancelled_at()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
BEGIN
  IF NEW.status = 'cancelado' AND (OLD.status IS DISTINCT FROM 'cancelado') THEN
    NEW.cancelled_at := COALESCE(NEW.cancelled_at, now());
  ELSIF NEW.status = 'disponible' THEN
    NEW.cancelled_at := NULL;
  END IF;
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_set_listing_cancelled_at ON public.recyclable_material_listings;
CREATE TRIGGER trg_set_listing_cancelled_at
  BEFORE UPDATE ON public.recyclable_material_listings
  FOR EACH ROW
  EXECUTE FUNCTION public.set_listing_cancelled_at();

-- Registra automáticamente la marca de tiempo cuando una orden de recolección pasa a estado cancelado
CREATE OR REPLACE FUNCTION public.set_pickup_cancelled_at()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
BEGIN
  IF NEW.status = 'cancelado' AND (OLD.status IS DISTINCT FROM 'cancelado') THEN
    NEW.cancelled_at := COALESCE(NEW.cancelled_at, now());
  END IF;
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_set_pickup_cancelled_at ON public.recycler_pickups;
CREATE TRIGGER trg_set_pickup_cancelled_at
  BEFORE UPDATE ON public.recycler_pickups
  FOR EACH ROW
  EXECUTE FUNCTION public.set_pickup_cancelled_at();

-- Sincroniza el estado de la publicación de material y emite la notificación correspondiente al reservar o cancelar
CREATE OR REPLACE FUNCTION public.sync_listing_and_notify_on_pickup_change()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'auth'
AS $function$
DECLARE
  v_owner_id uuid;
  v_material_type text;
BEGIN
  SELECT owner_id, material_type
    INTO v_owner_id, v_material_type
  FROM public.recyclable_material_listings
  WHERE id = NEW.listing_id;

  -- Cuando se crea una orden pendiente, marca la oferta como reservada y avisa al ciudadano dueño
  IF TG_OP = 'INSERT' AND NEW.status = 'pendiente' THEN
    UPDATE public.recyclable_material_listings
    SET status = 'reservado',
        cancelled_at = NULL
    WHERE id = NEW.listing_id
      AND status = 'disponible';

    IF v_owner_id IS NOT NULL THEN
      INSERT INTO public.notifications (profile_id, title, body, type, data)
      VALUES (
        v_owner_id,
        'Recolección reservada',
        'Un reciclador ha tomado tu publicación de ' || COALESCE(v_material_type, 'material reciclable') || '.',
        'recoleccion_reservada',
        jsonb_build_object('listing_id', NEW.listing_id, 'pickup_id', NEW.id)
      );
    END IF;

  -- Cuando una orden pendiente se cancela, actualiza la oferta según quién originó la cancelación
  ELSIF TG_OP = 'UPDATE' AND OLD.status = 'pendiente' AND NEW.status = 'cancelado' THEN
    IF auth.uid() = v_owner_id THEN
      -- Si cancela el ciudadano dueño, la publicación queda cerrada como cancelada y se avisa al reciclador
      UPDATE public.recyclable_material_listings
      SET status = 'cancelado',
          cancelled_at = COALESCE(cancelled_at, now())
      WHERE id = NEW.listing_id
        AND status <> 'cancelado';

      INSERT INTO public.notifications (profile_id, title, body, type, data)
      VALUES (
        NEW.recycler_id,
        'Entrega cancelada por el ciudadano',
        'El ciudadano canceló la entrega de ' || COALESCE(v_material_type, 'material reciclable') || '.',
        'recoleccion_cancelada',
        jsonb_build_object('listing_id', NEW.listing_id, 'pickup_id', NEW.id)
      );
    ELSE
      -- Si cancela el reciclador, la publicación regresa a disponible en el mapa y se avisa al ciudadano
      UPDATE public.recyclable_material_listings
      SET status = 'disponible',
          cancelled_at = NULL
      WHERE id = NEW.listing_id
        AND status = 'reservado';

      IF v_owner_id IS NOT NULL THEN
        INSERT INTO public.notifications (profile_id, title, body, type, data)
        VALUES (
          v_owner_id,
          'Recolección cancelada',
          'El reciclador no pudo completar el retiro de ' || COALESCE(v_material_type, 'material reciclable') || '. Tu oferta volvió a estar disponible.',
          'recoleccion_cancelada',
          jsonb_build_object('listing_id', NEW.listing_id, 'pickup_id', NEW.id)
        );
      END IF;
    END IF;
  END IF;

  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_sync_listing_and_notify_on_pickup_change ON public.recycler_pickups;
CREATE TRIGGER trg_sync_listing_and_notify_on_pickup_change
  AFTER INSERT OR UPDATE OF status ON public.recycler_pickups
  FOR EACH ROW
  EXECUTE FUNCTION public.sync_listing_and_notify_on_pickup_change();

-- Permite que un reciclador aprobado reserve una publicación disponible generando la orden y el código QR
CREATE OR REPLACE FUNCTION public.reserve_material_listing(
  p_listing_id uuid
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'auth'
AS $function$
DECLARE
  v_recycler_id uuid := auth.uid();
  v_listing record;
  v_pickup_id uuid;
  v_qr_code text;
  v_expires_at timestamptz := now() + interval '24 hours';
BEGIN
  IF v_recycler_id IS NULL THEN
    RAISE EXCEPTION 'Debe iniciar sesión para reservar una entrega.';
  END IF;

  IF NOT public.is_approved_recycler(v_recycler_id) THEN
    RAISE EXCEPTION 'Solo los recicladores aprobados pueden reservar material.';
  END IF;

  SELECT * INTO v_listing
  FROM public.recyclable_material_listings
  WHERE id = p_listing_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'La publicación de material no existe.';
  END IF;

  IF v_listing.status <> 'disponible' THEN
    RAISE EXCEPTION 'Esta publicación ya no se encuentra disponible (estado actual: %).', v_listing.status;
  END IF;

  v_qr_code := 'ECO-PKP-' || upper(substr(md5(random()::text || clock_timestamp()::text), 1, 8));

  INSERT INTO public.recycler_pickups (
    listing_id,
    recycler_id,
    qr_code,
    qr_expires_at,
    status
  )
  VALUES (
    p_listing_id,
    v_recycler_id,
    v_qr_code,
    v_expires_at,
    'pendiente'
  )
  RETURNING id INTO v_pickup_id;

  RETURN json_build_object(
    'success', true,
    'pickup_id', v_pickup_id,
    'listing_id', p_listing_id,
    'qr_code', v_qr_code,
    'qr_expires_at', v_expires_at,
    'message', 'Recolección reservada exitosamente.'
  );
END;
$function$;

-- Permite cancelar una recolección pendiente verificando que el solicitante sea el reciclador o el ciudadano dueño
CREATE OR REPLACE FUNCTION public.cancel_recycler_pickup(
  p_pickup_id uuid
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'auth'
AS $function$
DECLARE
  v_user_id uuid := auth.uid();
  v_pickup record;
  v_owner_id uuid;
BEGIN
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'Usuario no autenticado.';
  END IF;

  SELECT * INTO v_pickup
  FROM public.recycler_pickups
  WHERE id = p_pickup_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'La orden de recolección no existe.';
  END IF;

  SELECT owner_id INTO v_owner_id
  FROM public.recyclable_material_listings
  WHERE id = v_pickup.listing_id;

  IF v_user_id <> v_pickup.recycler_id AND v_user_id <> v_owner_id THEN
    RAISE EXCEPTION 'No tiene permisos para cancelar esta orden de recolección.';
  END IF;

  IF v_pickup.status <> 'pendiente' THEN
    RAISE EXCEPTION 'Solo se pueden cancelar recolecciones en estado pendiente (estado actual: %).', v_pickup.status;
  END IF;

  UPDATE public.recycler_pickups
  SET status = 'cancelado',
      cancelled_at = now()
  WHERE id = p_pickup_id;

  RETURN json_build_object(
    'success', true,
    'pickup_id', p_pickup_id,
    'listing_id', v_pickup.listing_id,
    'cancelled_at', now(),
    'message', 'Recolección cancelada exitosamente.'
  );
END;
$function$;

-- Políticas de seguridad para permitir inserción y cancelación directa de órdenes sobre recycler_pickups
DROP POLICY IF EXISTS "Recicladores aprobados crean ordenes de recoleccion" ON public.recycler_pickups;
CREATE POLICY "Recicladores aprobados crean ordenes de recoleccion" ON public.recycler_pickups
  FOR INSERT TO authenticated
  WITH CHECK (
    auth.uid() = recycler_id
    AND public.is_approved_recycler(auth.uid())
  );

DROP POLICY IF EXISTS "Recicladores cancelan sus ordenes de recoleccion pendientes" ON public.recycler_pickups;
CREATE POLICY "Recicladores cancelan sus ordenes de recoleccion pendientes" ON public.recycler_pickups
  FOR UPDATE TO authenticated
  USING (auth.uid() = recycler_id AND status = 'pendiente')
  WITH CHECK (auth.uid() = recycler_id AND status = 'cancelado');

DROP POLICY IF EXISTS "Ciudadanos cancelan recolecciones de sus ofertas" ON public.recycler_pickups;
CREATE POLICY "Ciudadanos cancelan recolecciones de sus ofertas" ON public.recycler_pickups
  FOR UPDATE TO authenticated
  USING (
    status = 'pendiente'
    AND EXISTS (
      SELECT 1 FROM public.recyclable_material_listings l
      WHERE l.id = recycler_pickups.listing_id AND l.owner_id = auth.uid()
    )
  )
  WITH CHECK (status = 'cancelado');

-- Concede permisos de ejecución a usuarios autenticados sobre las nuevas funciones RPC
GRANT EXECUTE ON FUNCTION public.reserve_material_listing(uuid) TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.cancel_recycler_pickup(uuid) TO authenticated, service_role;
