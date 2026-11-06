SET local check_function_bodies = off;

CREATE OR REPLACE FUNCTION public.can_citizen_see_recycler (
  r_profile_id uuid,
  citizen_id   uuid
)
  RETURNS boolean
  LANGUAGE sql
  SECURITY DEFINER
  SET search_path TO 'public'
  AS $function$
  SELECT EXISTS (
    SELECT 1 FROM public.recyclable_material_listings l
    JOIN public.recycler_pickups p ON p.listing_id = l.id
    WHERE p.recycler_id = r_profile_id AND l.owner_id = citizen_id
  );
$function$;

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
  SET search_path TO 'public', 'auth'
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

CREATE OR REPLACE FUNCTION public.is_admin (
  user_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  SECURITY DEFINER
  SET search_path TO 'public'
  AS $function$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = user_id AND role_code = 'admin'
  );
$function$;

CREATE OR REPLACE FUNCTION public.is_approved_recycler (
  user_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  SECURITY DEFINER
  SET search_path TO 'public'
  AS $function$
  SELECT EXISTS (
    SELECT 1 FROM public.recyclers
    WHERE profile_id = user_id AND status = 'aprobado'
  );
$function$;

CREATE OR REPLACE FUNCTION public.is_company_operator (
  c_id    uuid,
  user_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  SECURITY DEFINER
  SET search_path TO 'public'
  AS $function$
  SELECT CASE 
    WHEN c_id IS NULL OR user_id IS NULL THEN false
    ELSE EXISTS (
      SELECT 1 FROM public.operators
      WHERE company_id = c_id AND profile_id = user_id
    )
  END;
$function$;

CREATE OR REPLACE FUNCTION public.is_company_owner (
  c_id    uuid,
  user_id uuid
)
  RETURNS boolean
  LANGUAGE sql
  SECURITY DEFINER
  SET search_path TO 'public'
  AS $function$
  SELECT CASE 
    WHEN c_id IS NULL OR user_id IS NULL THEN false
    ELSE EXISTS (
      SELECT 1 FROM public.companies
      WHERE id = c_id AND owner_profile_id = user_id
    )
  END;
$function$;

CREATE OR REPLACE FUNCTION public.redeem_coupon (
  p_coupon_id uuid
)
  RETURNS json
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO 'public', 'auth'
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

CREATE OR REPLACE FUNCTION public.switch_active_role (
  p_role_code text
)
  RETURNS json
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO 'public', 'auth'
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

CREATE OR REPLACE FUNCTION public.validate_coupon_redemption (
  p_code text
)
  RETURNS json
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO 'public', 'auth'
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

