SET local check_function_bodies = off;

CREATE EXTENSION "postgis" SCHEMA "public";

CREATE TABLE "public"."bulky_waste_requests" (
  "id"             uuid                         NOT NULL DEFAULT gen_random_uuid(),
  "citizen_id"     uuid                         NOT NULL,
  "item_type"      text                         NOT NULL,
  "description"    text,
  "location"       public.geography(Point,4326) NOT NULL,
  "scheduled_date" date,
  "status"         text                         NOT NULL DEFAULT 'pendiente'::text,
  "cost"           numeric,
  "created_at"     timestamp with time zone     NOT NULL DEFAULT now(),
  "updated_at"     timestamp with time zone     NOT NULL DEFAULT now(),
  "company_id"     uuid,
  CONSTRAINT "bulky_waste_requests_pkey" PRIMARY KEY (id),
  CONSTRAINT "bulky_waste_requests_status_check" CHECK ((status = ANY (ARRAY['pendiente'::text, 'cotizado'::text, 'programado'::text, 'recolectado'::text, 'cancelado'::text])))
);

ALTER TABLE "public"."bulky_waste_requests"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."businesses" (
  "profile_id"    uuid                     NOT NULL,
  "business_name" text                     NOT NULL,
  "category"      text,
  "address"       text,
  "status"        text                     NOT NULL DEFAULT 'pendiente'::text,
  "created_at"    timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"    timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "businesses_pkey" PRIMARY KEY (profile_id),
  CONSTRAINT "businesses_status_check" CHECK ((status = ANY (ARRAY['pendiente'::text, 'aprobado'::text, 'rechazado'::text])))
);

ALTER TABLE "public"."businesses"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."citizens" (
  "profile_id"                 uuid                         NOT NULL,
  "phone"                      text,
  "neighborhood"               text,
  "ecopoints_balance"          integer                      NOT NULL DEFAULT 0,
  "updated_at"                 timestamp with time zone     NOT NULL DEFAULT now(),
  "notification_radius_meters" integer                      NOT NULL DEFAULT 300,
  "home_location"              public.geography(Point,4326),
  CONSTRAINT "chk_ecopoints_non_negative" CHECK ((ecopoints_balance >= 0)),
  CONSTRAINT "citizens_notification_radius_meters_check" CHECK (((notification_radius_meters >= 50) AND (notification_radius_meters <= 2000))),
  CONSTRAINT "citizens_pkey" PRIMARY KEY (profile_id)
);

ALTER TABLE "public"."citizens"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."collection_points" (
  "id"           uuid                         NOT NULL DEFAULT gen_random_uuid(),
  "label"        text                         NOT NULL,
  "location"     public.geography(Point,4326) NOT NULL,
  "neighborhood" text,
  "created_at"   timestamp with time zone     NOT NULL DEFAULT now(),
  "updated_at"   timestamp with time zone     NOT NULL DEFAULT now(),
  CONSTRAINT "collection_points_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."collection_points"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."companies" (
  "id"         uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "name"       text                     NOT NULL,
  "type"       text[]                   NOT NULL,
  "created_at" timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at" timestamp with time zone NOT NULL DEFAULT now(),
  "nit"        text,
  "address"    text,
  "phone"      text,
  "email"      text,
  CONSTRAINT "companies_nit_key" UNIQUE (nit),
  CONSTRAINT "companies_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."companies"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."coupon_redemptions" (
  "id"          uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "citizen_id"  uuid                     NOT NULL,
  "coupon_id"   uuid                     NOT NULL,
  "redeemed_at" timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "coupon_redemptions_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."coupon_redemptions"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."coupons" (
  "id"                 uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "business_id"        uuid                     NOT NULL,
  "title"              text                     NOT NULL,
  "points_cost"        integer                  NOT NULL,
  "quantity_available" integer                  NOT NULL DEFAULT 0,
  "valid_until"        date,
  "is_active"          boolean                  NOT NULL DEFAULT true,
  "created_at"         timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"         timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "coupons_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."coupons"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."ecopoints_transactions" (
  "id"            uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "citizen_id"    uuid                     NOT NULL,
  "points"        integer                  NOT NULL,
  "reason_code"   text                     NOT NULL,
  "pickup_id"     uuid,
  "completion_id" uuid,
  "redemption_id" uuid,
  "created_at"    timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "chk_transaction_source" CHECK (((num_nonnulls(pickup_id, completion_id, redemption_id) <= 1) AND (((reason_code = 'entrega_material'::text) AND (pickup_id IS
    NOT NULL)) OR ((reason_code = 'capacitacion'::text) AND (completion_id IS NOT NULL)) OR ((reason_code = 'redencion_cupon'::text) AND (redemption_id IS
    NOT NULL)) OR (reason_code <> ALL (ARRAY['entrega_material'::text, 'capacitacion'::text, 'redencion_cupon'::text]))))),
  CONSTRAINT "ecopoints_transactions_completion_id_key" UNIQUE (completion_id),
  CONSTRAINT "ecopoints_transactions_pickup_id_key" UNIQUE (pickup_id),
  CONSTRAINT "ecopoints_transactions_pkey" PRIMARY KEY (id),
  CONSTRAINT "ecopoints_transactions_redemption_id_key" UNIQUE (redemption_id)
);

ALTER TABLE "public"."ecopoints_transactions"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."educational_completions" (
  "id"           uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "citizen_id"   uuid                     NOT NULL,
  "content_id"   uuid                     NOT NULL,
  "completed_at" timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "educational_completions_citizen_id_content_id_key" UNIQUE (citizen_id, content_id),
  CONSTRAINT "educational_completions_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."educational_completions"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."educational_content" (
  "id"            uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "title"         text                     NOT NULL,
  "content_type"  text                     NOT NULL,
  "points_reward" integer                  NOT NULL DEFAULT 0,
  "is_active"     boolean                  NOT NULL DEFAULT true,
  "created_at"    timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"    timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "educational_content_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."educational_content"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."operators" (
  "profile_id"    uuid                     NOT NULL,
  "company_id"    uuid                     NOT NULL,
  "updated_at"    timestamp with time zone NOT NULL DEFAULT now(),
  "operator_type" text                     NOT NULL DEFAULT 'despachador'::text,
  CONSTRAINT "operators_operator_type_check" CHECK ((operator_type = ANY (ARRAY['despachador'::text, 'conductor'::text]))),
  CONSTRAINT "operators_pkey" PRIMARY KEY (profile_id)
);

ALTER TABLE "public"."operators"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."priority_weights" (
  "id"         uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "company_id" uuid                     NOT NULL,
  "w1"         numeric                  NOT NULL DEFAULT 0.4,
  "w2"         numeric                  NOT NULL DEFAULT 0.3,
  "w3"         numeric                  NOT NULL DEFAULT 0.2,
  "w4"         numeric                  NOT NULL DEFAULT 0.1,
  "updated_at" timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "priority_weights_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."priority_weights"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."profiles" (
  "id"                   uuid                     NOT NULL,
  "document_id"          text,
  "role_code"            text                     NOT NULL,
  "full_name"            text                     NOT NULL,
  "email"                text                     NOT NULL,
  "created_at"           timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"           timestamp with time zone NOT NULL DEFAULT now(),
  "must_change_password" boolean                  NOT NULL DEFAULT false,
  CONSTRAINT "profiles_document_id_key" UNIQUE (document_id),
  CONSTRAINT "profiles_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."profiles"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."recyclable_material_listings" (
  "id"                uuid                         NOT NULL DEFAULT gen_random_uuid(),
  "owner_id"          uuid                         NOT NULL,
  "material_type"     text                         NOT NULL,
  "quantity_category" text                         NOT NULL,
  "location"          public.geography(Point,4326) NOT NULL,
  "status"            text                         NOT NULL DEFAULT 'disponible'::text,
  "created_at"        timestamp with time zone     NOT NULL DEFAULT now(),
  "updated_at"        timestamp with time zone     NOT NULL DEFAULT now(),
  CONSTRAINT "recyclable_material_listings_pkey" PRIMARY KEY (id),
  CONSTRAINT "recyclable_material_listings_status_check" CHECK ((status = ANY (ARRAY['disponible'::text, 'reservado'::text, 'completado'::text, 'cancelado'::text])))
);

ALTER TABLE "public"."recyclable_material_listings"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."recycler_pickups" (
  "id"                        uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "listing_id"                uuid                     NOT NULL,
  "recycler_id"               uuid                     NOT NULL,
  "qr_code"                   text                     NOT NULL,
  "qr_expires_at"             timestamp with time zone NOT NULL,
  "geolocation_verified"      boolean                  NOT NULL DEFAULT false,
  "confirmed_volume_category" text,
  "status"                    text                     NOT NULL DEFAULT 'pendiente'::text,
  "created_at"                timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"                timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "recycler_pickups_pkey" PRIMARY KEY (id),
  CONSTRAINT "recycler_pickups_qr_code_key" UNIQUE (qr_code),
  CONSTRAINT "recycler_pickups_status_check" CHECK ((status = ANY (ARRAY['pendiente'::text, 'completado'::text, 'cancelado'::text])))
);

ALTER TABLE "public"."recycler_pickups"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."recyclers" (
  "profile_id"       uuid                     NOT NULL,
  "phone"            text,
  "association_name" text,
  "status"           text                     NOT NULL DEFAULT 'pendiente'::text,
  "created_at"       timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at"       timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "recyclers_pkey" PRIMARY KEY (profile_id),
  CONSTRAINT "recyclers_status_check" CHECK ((status = ANY (ARRAY['pendiente'::text, 'aprobado'::text, 'rechazado'::text])))
);

ALTER TABLE "public"."recyclers"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."reports" (
  "id"           uuid                         NOT NULL DEFAULT gen_random_uuid(),
  "citizen_id"   uuid                         NOT NULL,
  "point_id"     uuid,
  "type"         text                         NOT NULL,
  "photo_url"    text,
  "location"     public.geography(Point,4326) NOT NULL,
  "status"       text                         NOT NULL DEFAULT 'pendiente'::text,
  "validated_by" uuid,
  "created_at"   timestamp with time zone     NOT NULL DEFAULT now(),
  "updated_at"   timestamp with time zone     NOT NULL DEFAULT now(),
  CONSTRAINT "reports_pkey" PRIMARY KEY (id),
  CONSTRAINT "reports_status_check" CHECK ((status = ANY (ARRAY['pendiente'::text, 'aprobado'::text, 'rechazado'::text, 'duplicado'::text, 'atendido'::text]))),
  CONSTRAINT "reports_type_check" CHECK ((type = ANY (ARRAY['contenedor_lleno'::text, 'botadero_clandestino'::text])))
);

ALTER TABLE "public"."reports"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."roles" (
  "code"        text                     NOT NULL,
  "name"        text                     NOT NULL,
  "description" text,
  "is_active"   boolean                  NOT NULL DEFAULT true,
  "created_at"  timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "roles_pkey" PRIMARY KEY (code)
);

ALTER TABLE "public"."roles"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."route_points" (
  "id"             uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "route_id"       uuid                     NOT NULL,
  "point_id"       uuid                     NOT NULL,
  "sequence_order" integer                  NOT NULL,
  "priority_score" numeric,
  "visited"        boolean                  NOT NULL DEFAULT false,
  "visited_at"     timestamp with time zone,
  CONSTRAINT "route_points_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."route_points"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."routes" (
  "id"         uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "company_id" uuid                     NOT NULL,
  "created_by" uuid                     NOT NULL,
  "route_date" date                     NOT NULL,
  "status"     text                     NOT NULL DEFAULT 'borrador'::text,
  "created_at" timestamp with time zone NOT NULL DEFAULT now(),
  "updated_at" timestamp with time zone NOT NULL DEFAULT now(),
  "route_type" text                     NOT NULL DEFAULT 'dinamica'::text,
  CONSTRAINT "routes_pkey" PRIMARY KEY (id),
  CONSTRAINT "routes_route_type_check" CHECK ((route_type = ANY (ARRAY['ordinaria'::text, 'dinamica'::text]))),
  CONSTRAINT "routes_status_check" CHECK ((status = ANY (ARRAY['borrador'::text, 'activa'::text, 'completada'::text, 'cancelada'::text])))
);

ALTER TABLE "public"."routes"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."transaction_reasons" (
  "code"           text                     NOT NULL,
  "name"           text                     NOT NULL,
  "type"           text                     NOT NULL,
  "default_points" integer,
  "is_active"      boolean                  NOT NULL DEFAULT true,
  "created_at"     timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "transaction_reasons_pkey" PRIMARY KEY (code),
  CONSTRAINT "transaction_reasons_type_check" CHECK ((type = ANY (ARRAY['earn'::text, 'spend'::text])))
);

ALTER TABLE "public"."transaction_reasons"
  ENABLE ROW LEVEL SECURITY;

CREATE TABLE "public"."weekly_pickup_capacities" (
  "id"          uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "company_id"  uuid                     NOT NULL,
  "day_of_week" integer                  NOT NULL,
  "max_slots"   integer                  NOT NULL DEFAULT 10,
  "updated_at"  timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "weekly_pickup_capacities_company_id_day_of_week_key" UNIQUE (company_id, day_of_week),
  CONSTRAINT "weekly_pickup_capacities_day_of_week_check" CHECK (((day_of_week >= 1) AND (day_of_week <= 7))),
  CONSTRAINT "weekly_pickup_capacities_max_slots_check" CHECK ((max_slots >= 0)),
  CONSTRAINT "weekly_pickup_capacities_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."weekly_pickup_capacities"
  ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.enforce_bulky_request_status_transition()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  AS $function$
BEGIN
  IF OLD.status IS DISTINCT FROM NEW.status THEN

    IF OLD.status = 'pendiente' AND NEW.status NOT IN ('cotizado', 'cancelado') THEN
      RAISE EXCEPTION 'Transición inválida: una solicitud "pendiente" solo puede pasar a "cotizado" o "cancelado".';
    
    ELSIF OLD.status = 'cotizado' AND NEW.status NOT IN ('programado', 'cancelado') THEN
      RAISE EXCEPTION 'Transición inválida: una solicitud "cotizada" solo puede pasar a "programado" o "cancelado".';
    
    ELSIF OLD.status = 'programado' AND NEW.status NOT IN ('recolectado', 'cancelado') THEN
      RAISE EXCEPTION 'Transición inválida: una solicitud "programada" solo puede pasar a "recolectado" o "cancelado".';
    
    ELSIF OLD.status IN ('recolectado', 'cancelado') THEN
      RAISE EXCEPTION 'La solicitud ya fue "%" y no se puede modificar.', OLD.status;
    END IF;

  END IF;

  RETURN NEW;
END;
$function$;

CREATE OR REPLACE FUNCTION public.enforce_report_status_transition()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  AS $function$
BEGIN
  -- Validar solo cuando el campo status esté cambiando
  IF OLD.status IS DISTINCT FROM NEW.status THEN
    
    -- Reglas de transición desde 'pendiente'
    IF OLD.status = 'pendiente' AND NEW.status NOT IN ('aprobado', 'rechazado', 'duplicado') THEN
      RAISE EXCEPTION 'Transición no permitida: de "pendiente" solo se puede pasar a "aprobado", "rechazado" o "duplicado".';
    
    -- Reglas de transición desde 'aprobado'
    ELSIF OLD.status = 'aprobado' AND NEW.status NOT IN ('atendido') THEN
      RAISE EXCEPTION 'Transición no permitida: de "aprobado" solo se puede pasar a "atendido".';
    
    -- Estados terminales que no permiten más cambios
    ELSIF OLD.status IN ('rechazado', 'duplicado', 'atendido') THEN
      RAISE EXCEPTION 'El reporte ya se encuentra en un estado final ("%") y no puede ser modificado.', OLD.status;
    END IF;
  END IF;
  RETURN NEW;
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
  AS $function$
DECLARE
  v_company_id uuid;
  v_w1 numeric;
  v_w2 numeric;
  v_w3 numeric;
  v_w4 numeric;
BEGIN
  -- A. Identificar la empresa a la que pertenece el operador que hace la consulta
  SELECT company_id INTO v_company_id
  FROM operators
  WHERE profile_id = auth.uid();

  -- B. Obtener los pesos configurados (w1, w2, w3, w4) para esa empresa
  SELECT w1, w2, w3, w4 INTO v_w1, v_w2, v_w3, v_w4
  FROM priority_weights
  WHERE company_id = v_company_id;

  -- Validador: Si la empresa no ha configurado sus pesos, asigna los por defecto
  IF NOT FOUND THEN
    v_w1 := 0.4; v_w2 := 0.3; v_w3 := 0.2; v_w4 := 0.1;
  END IF;

  -- C. Calcular el puntaje final ordenando de mayor a menor urgencia
  RETURN QUERY
  SELECT 
    v.point_id,
    v.label,
    v.neighborhood,
    v.location,
    v.approved_reports_count,
    v.zone_density,
    v.oldest_report_days,
    
    -- Distancia en Kilómetros desde el origen hasta el punto
    CASE 
      WHEN p_origin_location IS NOT NULL THEN (ST_Distance(v.location, p_origin_location) / 1000.0)::numeric
      ELSE 0
    END AS distance_km,
    
    -- APLICACIÓN DE LA FÓRMULA PONDERADA COMPLETA CON W4
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

CREATE OR REPLACE FUNCTION public.redeem_coupon (
  p_coupon_id uuid
)
  RETURNS json
  LANGUAGE plpgsql
  SECURITY DEFINER
  AS $function$
DECLARE
  v_citizen_id uuid := auth.uid();
  v_coupon record;
  v_balance int;
  v_redemption_id uuid;
BEGIN
  -- A. Consultar cupón
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

  -- B. Verificar saldo del ciudadano
  SELECT ecopoints_balance INTO v_balance FROM citizens WHERE profile_id = v_citizen_id;
  IF v_balance < v_coupon.points_cost THEN
    RAISE EXCEPTION 'Saldo insuficiente de EcoPuntos.';
  END IF;

  -- C. Decrementar stock
  UPDATE coupons 
  SET quantity_available = quantity_available - 1 
  WHERE id = p_coupon_id;

  -- D. Registrar redención
  INSERT INTO coupon_redemptions (citizen_id, coupon_id)
  VALUES (v_citizen_id, p_coupon_id)
  RETURNING id INTO v_redemption_id;

  -- E. Registrar transacción negativa (El trigger resta el saldo en la tabla citizens)
  INSERT INTO ecopoints_transactions (citizen_id, points, reason_code, redemption_id)
  VALUES (v_citizen_id, -abs(v_coupon.points_cost), 'redencion_cupon', v_redemption_id);

  RETURN json_build_object(
    'success', true,
    'redemption_id', v_redemption_id,
    'message', 'Cupón redimido exitosamente.'
  );
END;
$function$;

CREATE OR REPLACE FUNCTION public.sync_citizen_ecopoints_balance()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  AS $function$
declare
  v_reason_type text;
  v_delta       int;
begin
  select type into v_reason_type 
  from transaction_reasons 
  where code = NEW.reason_code;

  if v_reason_type = 'spend' then
    v_delta := -abs(NEW.points);
  else
    v_delta := abs(NEW.points);
  end if;

  -- Actualiza el saldo en la tabla citizens.
  -- Si ecopoints_balance pasa a ser menor a 0, la regla
  -- CHECK (ecopoints_balance >= 0) cancelará la inserción automáticamente.
  update citizens
  set ecopoints_balance = ecopoints_balance + v_delta
  where profile_id = NEW.citizen_id;

  return NEW;
end;
$function$;

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  AS $function$
begin
  new.updated_at = now();
  return new;
end;
$function$;

CREATE OR REPLACE FUNCTION public.validate_and_credit_pickup (
  p_pickup_id                 uuid,
  p_scanned_qr                text,
  p_recycler_lat              numeric,
  p_recycler_lng              numeric,
  p_confirmed_volume_category text
)
  RETURNS json
  LANGUAGE plpgsql
  SECURITY DEFINER
  AS $function$
DECLARE
  v_pickup record;
  v_listing record;
  v_recycler_loc geography;
  v_points_to_award int;
  v_multiplier int;
BEGIN
  -- A. Buscar la orden de recolección
  SELECT * INTO v_pickup FROM recycler_pickups WHERE id = p_pickup_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'La orden de recolección no existe.';
  END IF;

  -- B. Validar estado y código QR
  IF v_pickup.status <> 'pendiente' THEN
    RAISE EXCEPTION 'Esta entrega ya fue procesada o cancelada.';
  END IF;

  IF v_pickup.qr_code <> p_scanned_qr THEN
    RAISE EXCEPTION 'El código QR escaneado es inválido.';
  END IF;

  IF NOW() > v_pickup.qr_expires_at THEN
    RAISE EXCEPTION 'El código QR ha expirado.';
  END IF;

  -- C. Validar geolocalización (Distancia máxima permitida: 100 metros)
  SELECT * INTO v_listing FROM recyclable_material_listings WHERE id = v_pickup.listing_id;
  v_recycler_loc := ST_SetSRID(ST_MakePoint(p_recycler_lng, p_recycler_lat), 4326)::geography;

  IF ST_Distance(v_listing.location, v_recycler_loc) > 100 THEN
    RAISE EXCEPTION 'Ubicación no verificada: Debe estar a menos de 100m de la entrega.';
  END IF;

  -- D. Calcular puntos según categoría de volumen
  v_multiplier := CASE p_confirmed_volume_category
    WHEN 'alto' THEN 3
    WHEN 'medio' THEN 2
    ELSE 1
  END;
  v_points_to_award := 10 * v_multiplier;

  -- E. Actualizar estado de la recolección
  UPDATE recycler_pickups
  SET status = 'completado',
      geolocation_verified = true,
      confirmed_volume_category = p_confirmed_volume_category
  WHERE id = p_pickup_id;

  -- F. Marcar la publicación como completada
  UPDATE recyclable_material_listings
  SET status = 'completado'
  WHERE id = v_pickup.listing_id;

  -- G. Registrar la transacción de EcoPuntos (El trigger sync_citizen_ecopoints_balance sumará el saldo)
  INSERT INTO ecopoints_transactions (citizen_id, points, reason_code, pickup_id)
  VALUES (v_listing.owner_id, v_points_to_award, 'entrega_material', p_pickup_id);

  RETURN json_build_object(
    'success', true,
    'points_awarded', v_points_to_award,
    'message', 'Entrega validada y EcoPuntos acreditados exitosamente.'
  );
END;
$function$;

ALTER TABLE "public"."bulky_waste_requests"
  ADD CONSTRAINT "bulky_waste_requests_citizen_id_fkey" FOREIGN KEY (citizen_id) REFERENCES public.citizens(profile_id);

ALTER TABLE "public"."bulky_waste_requests"
  ADD CONSTRAINT "bulky_waste_requests_company_id_fkey" FOREIGN KEY (company_id) REFERENCES public.companies(id);

ALTER TABLE "public"."coupon_redemptions"
  ADD CONSTRAINT "coupon_redemptions_citizen_id_fkey" FOREIGN KEY (citizen_id) REFERENCES public.citizens(profile_id);

ALTER TABLE "public"."coupons"
  ADD CONSTRAINT "coupons_business_id_fkey" FOREIGN KEY (business_id) REFERENCES public.businesses(profile_id);

ALTER TABLE "public"."coupon_redemptions"
  ADD CONSTRAINT "coupon_redemptions_coupon_id_fkey" FOREIGN KEY (coupon_id) REFERENCES public.coupons(id);

ALTER TABLE "public"."ecopoints_transactions"
  ADD CONSTRAINT "ecopoints_transactions_citizen_id_fkey" FOREIGN KEY (citizen_id) REFERENCES public.citizens(profile_id);

ALTER TABLE "public"."ecopoints_transactions"
  ADD CONSTRAINT "ecopoints_transactions_redemption_id_fkey" FOREIGN KEY (redemption_id) REFERENCES public.coupon_redemptions(id) ON DELETE SET NULL;

ALTER TABLE "public"."educational_completions"
  ADD CONSTRAINT "educational_completions_citizen_id_fkey" FOREIGN KEY (citizen_id) REFERENCES public.citizens(profile_id);

ALTER TABLE "public"."ecopoints_transactions"
  ADD CONSTRAINT "ecopoints_transactions_completion_id_fkey" FOREIGN KEY (completion_id) REFERENCES public.educational_completions(id) ON DELETE SET NULL;

ALTER TABLE "public"."educational_completions"
  ADD CONSTRAINT "educational_completions_content_id_fkey" FOREIGN KEY (content_id) REFERENCES public.educational_content(id);

ALTER TABLE "public"."operators"
  ADD CONSTRAINT "operators_company_id_fkey" FOREIGN KEY (company_id) REFERENCES public.companies(id);

ALTER TABLE "public"."priority_weights"
  ADD CONSTRAINT "priority_weights_company_id_fkey" FOREIGN KEY (company_id) REFERENCES public.companies(id);

ALTER TABLE "public"."profiles"
  ADD CONSTRAINT "profiles_id_fkey" FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE;

ALTER TABLE "public"."businesses"
  ADD CONSTRAINT "businesses_profile_id_fkey" FOREIGN KEY (profile_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

ALTER TABLE "public"."citizens"
  ADD CONSTRAINT "citizens_profile_id_fkey" FOREIGN KEY (profile_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

ALTER TABLE "public"."operators"
  ADD CONSTRAINT "operators_profile_id_fkey" FOREIGN KEY (profile_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

ALTER TABLE "public"."recyclable_material_listings"
  ADD CONSTRAINT "recyclable_material_listings_owner_id_fkey" FOREIGN KEY (owner_id) REFERENCES public.profiles(id);

ALTER TABLE "public"."recycler_pickups"
  ADD CONSTRAINT "recycler_pickups_listing_id_fkey" FOREIGN KEY (listing_id) REFERENCES public.recyclable_material_listings(id);

ALTER TABLE "public"."ecopoints_transactions"
  ADD CONSTRAINT "ecopoints_transactions_pickup_id_fkey" FOREIGN KEY (pickup_id) REFERENCES public.recycler_pickups(id) ON DELETE SET NULL;

ALTER TABLE "public"."recycler_pickups"
  ADD CONSTRAINT "recycler_pickups_recycler_id_fkey" FOREIGN KEY (recycler_id) REFERENCES public.recyclers(profile_id);

ALTER TABLE "public"."recyclers"
  ADD CONSTRAINT "recyclers_profile_id_fkey" FOREIGN KEY (profile_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

ALTER TABLE "public"."reports"
  ADD CONSTRAINT "reports_citizen_id_fkey" FOREIGN KEY (citizen_id) REFERENCES public.citizens(profile_id);

ALTER TABLE "public"."reports"
  ADD CONSTRAINT "reports_point_id_fkey" FOREIGN KEY (point_id) REFERENCES public.collection_points(id);

ALTER TABLE "public"."reports"
  ADD CONSTRAINT "reports_validated_by_fkey" FOREIGN KEY (validated_by) REFERENCES public.operators(profile_id);

ALTER TABLE "public"."profiles"
  ADD CONSTRAINT "profiles_role_code_fkey" FOREIGN KEY (role_code) REFERENCES public.roles(code);

ALTER TABLE "public"."route_points"
  ADD CONSTRAINT "route_points_point_id_fkey" FOREIGN KEY (point_id) REFERENCES public.collection_points(id);

ALTER TABLE "public"."routes"
  ADD CONSTRAINT "routes_company_id_fkey" FOREIGN KEY (company_id) REFERENCES public.companies(id);

ALTER TABLE "public"."routes"
  ADD CONSTRAINT "routes_created_by_fkey" FOREIGN KEY (created_by) REFERENCES public.operators(profile_id);

ALTER TABLE "public"."route_points"
  ADD CONSTRAINT "route_points_route_id_fkey" FOREIGN KEY (route_id) REFERENCES public.routes(id) ON DELETE CASCADE;

ALTER TABLE "public"."ecopoints_transactions"
  ADD CONSTRAINT "ecopoints_transactions_reason_code_fkey" FOREIGN KEY (reason_code) REFERENCES public.transaction_reasons(code);

ALTER TABLE "public"."weekly_pickup_capacities"
  ADD CONSTRAINT "weekly_pickup_capacities_company_id_fkey" FOREIGN KEY (company_id) REFERENCES public.companies(id);

CREATE VIEW "public"."collection_points_priority_view" AS  SELECT cp.id AS point_id,
    cp.label,
    cp.neighborhood,
    cp.location,
    count(r.id) FILTER (WHERE (r.status = 'aprobado'::text)) AS approved_reports_count,
    ( SELECT count(*) AS count
           FROM public.reports r_near
          WHERE ((r_near.status = 'aprobado'::text) AND public.st_dwithin(r_near.location, cp.location, (500)::double precision))) AS zone_density,
    COALESCE((EXTRACT(epoch FROM (now() - min(r.created_at) FILTER (WHERE (r.status = 'aprobado'::text)))) / (86400)::numeric), (0)::numeric) AS oldest_report_days
   FROM (public.collection_points cp
     LEFT JOIN public.reports r ON ((r.point_id = cp.id)))
  GROUP BY cp.id, cp.label, cp.neighborhood, cp.location;

CREATE VIEW "public"."neighborhood_recycling_ranking_view" AS  SELECT c.neighborhood,
    count(rp.id) AS total_pickups_completed,
    count(DISTINCT rp.recycler_id) AS active_recyclers_count,
    count(DISTINCT c.profile_id) AS active_citizens_count
   FROM (((public.citizens c
     JOIN public.profiles p ON ((p.id = c.profile_id)))
     JOIN public.recyclable_material_listings l ON ((l.owner_id = p.id)))
     JOIN public.recycler_pickups rp ON ((rp.listing_id = l.id)))
  WHERE ((rp.status = 'completado'::text) AND (c.neighborhood IS NOT NULL))
  GROUP BY c.neighborhood
  ORDER BY (count(rp.id)) DESC;

CREATE INDEX bulky_waste_requests_citizen_id_idx ON public.bulky_waste_requests USING btree (citizen_id);

CREATE INDEX bulky_waste_requests_location_idx ON public.bulky_waste_requests USING gist (location);

CREATE INDEX bulky_waste_requests_scheduled_date_idx ON public.bulky_waste_requests USING btree (scheduled_date);

CREATE INDEX citizens_home_location_idx ON public.citizens USING gist (home_location);

CREATE INDEX collection_points_location_idx ON public.collection_points USING gist (location);

CREATE INDEX coupon_redemptions_citizen_id_idx ON public.coupon_redemptions USING btree (citizen_id);

CREATE INDEX coupon_redemptions_coupon_id_idx ON public.coupon_redemptions USING btree (coupon_id);

CREATE INDEX coupons_business_id_idx ON public.coupons USING btree (business_id);

CREATE INDEX ecopoints_transactions_citizen_id_idx ON public.ecopoints_transactions USING btree (citizen_id);

CREATE INDEX ecopoints_transactions_reason_code_idx ON public.ecopoints_transactions USING btree (reason_code);

CREATE INDEX educational_completions_citizen_id_idx ON public.educational_completions USING btree (citizen_id);

CREATE INDEX educational_completions_content_id_idx ON public.educational_completions USING btree (content_id);

CREATE INDEX operators_company_id_idx ON public.operators USING btree (company_id);

CREATE INDEX priority_weights_company_id_idx ON public.priority_weights USING btree (company_id);

CREATE INDEX recyclable_material_listings_location_idx ON public.recyclable_material_listings USING gist (location);

CREATE INDEX recyclable_material_listings_owner_id_idx ON public.recyclable_material_listings USING btree (owner_id);

CREATE INDEX recycler_pickups_listing_id_idx ON public.recycler_pickups USING btree (listing_id);

CREATE INDEX recycler_pickups_recycler_id_idx ON public.recycler_pickups USING btree (recycler_id);

CREATE INDEX reports_citizen_id_idx ON public.reports USING btree (citizen_id);

CREATE INDEX reports_location_idx ON public.reports USING gist (location);

CREATE INDEX reports_point_id_idx ON public.reports USING btree (point_id);

CREATE INDEX reports_status_created_at_idx ON public.reports USING btree (status, created_at);

CREATE INDEX reports_validated_by_idx ON public.reports USING btree (validated_by);

CREATE INDEX route_points_point_id_idx ON public.route_points USING btree (point_id);

CREATE INDEX route_points_route_id_idx ON public.route_points USING btree (route_id);

CREATE INDEX routes_company_id_idx ON public.routes USING btree (company_id);

CREATE INDEX routes_created_by_idx ON public.routes USING btree (created_by);

CREATE TRIGGER check_bulky_request_status_transition
  BEFORE UPDATE ON public.bulky_waste_requests
  FOR EACH ROW
  EXECUTE FUNCTION public.enforce_bulky_request_status_transition();

CREATE TRIGGER update_bulky_waste_requests_updated_at
  BEFORE UPDATE ON public.bulky_waste_requests
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_businesses_updated_at
  BEFORE UPDATE ON public.businesses
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_citizens_updated_at
  BEFORE UPDATE ON public.citizens
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_collection_points_updated_at
  BEFORE UPDATE ON public.collection_points
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_companies_updated_at
  BEFORE UPDATE ON public.companies
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_coupons_updated_at
  BEFORE UPDATE ON public.coupons
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER sync_citizen_ecopoints_balance_trigger
  BEFORE INSERT ON public.ecopoints_transactions
  FOR EACH ROW
  EXECUTE FUNCTION public.sync_citizen_ecopoints_balance();

CREATE TRIGGER update_educational_content_updated_at
  BEFORE UPDATE ON public.educational_content
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_operators_updated_at
  BEFORE UPDATE ON public.operators
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_priority_weights_updated_at
  BEFORE UPDATE ON public.priority_weights
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_profiles_updated_at
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_recyclable_material_listings_updated_at
  BEFORE UPDATE ON public.recyclable_material_listings
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_recycler_pickups_updated_at
  BEFORE UPDATE ON public.recycler_pickups
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_recyclers_updated_at
  BEFORE UPDATE ON public.recyclers
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER check_report_status_transition
  BEFORE UPDATE ON public.reports
  FOR EACH ROW
  EXECUTE FUNCTION public.enforce_report_status_transition();

CREATE TRIGGER update_reports_updated_at
  BEFORE UPDATE ON public.reports
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_routes_updated_at
  BEFORE UPDATE ON public.routes
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE POLICY "Ciudadanos ven sus redenciones" ON "public"."coupon_redemptions"
  FOR SELECT
  TO PUBLIC
  USING ((citizen_id = auth.uid()));

CREATE POLICY "Comercios gestionan sus propios cupones" ON "public"."coupons"
  FOR ALL
  TO PUBLIC
  USING ((business_id = auth.uid()));

CREATE POLICY "Lectura publica de cupones activos" ON "public"."coupons"
  FOR SELECT
  TO PUBLIC
  USING ((is_active = true));

CREATE POLICY "Ciudadanos ven su propio historial de ecopuntos" ON "public"."ecopoints_transactions"
  FOR SELECT
  TO PUBLIC
  USING ((auth.uid() = citizen_id));

CREATE POLICY "Ciudadanos registran sus capacitaciones" ON "public"."educational_completions"
  FOR INSERT
  TO PUBLIC
  WITH CHECK ((auth.uid() = citizen_id));

CREATE POLICY "Ciudadanos ven sus capacitaciones completadas" ON "public"."educational_completions"
  FOR SELECT
  TO PUBLIC
  USING ((auth.uid() = citizen_id));

CREATE POLICY "Usuarios ven contenido educativo activo" ON "public"."educational_content"
  FOR SELECT
  TO PUBLIC
  USING ((is_active = true));

CREATE POLICY "Operadores leen y actualizan pesos de su empresa" ON "public"."priority_weights"
  FOR ALL
  TO PUBLIC
  USING ((EXISTS ( SELECT 1
   FROM public.operators
  WHERE ((operators.profile_id = auth.uid()) AND (operators.company_id = priority_weights.company_id)))));

CREATE POLICY "Los usuarios actualizan su propio perfil" ON "public"."profiles"
  FOR UPDATE
  TO PUBLIC
  USING ((auth.uid() = id));

CREATE POLICY "Los usuarios ven su propio perfil" ON "public"."profiles"
  FOR SELECT
  TO PUBLIC
  USING ((auth.uid() = id));

CREATE POLICY "Recicladores aprobados ven ofertas disponibles" ON "public"."recyclable_material_listings"
  FOR SELECT
  TO PUBLIC
  USING (((status = 'disponible'::text) AND (EXISTS ( SELECT 1
   FROM public.recyclers
  WHERE ((recyclers.profile_id = auth.uid()) AND (recyclers.status = 'aprobado'::text))))));

CREATE POLICY "Ciudadanos crean reportes" ON "public"."reports"
  FOR INSERT
  TO PUBLIC
  WITH CHECK ((auth.uid() = citizen_id));

CREATE POLICY "Ciudadanos ven sus propios reportes" ON "public"."reports"
  FOR SELECT
  TO PUBLIC
  USING ((auth.uid() = citizen_id));

CREATE POLICY "Operadores moderan reportes" ON "public"."reports"
  FOR UPDATE
  TO PUBLIC
  USING ((EXISTS ( SELECT 1
   FROM public.profiles
  WHERE ((profiles.id = auth.uid()) AND (profiles.role_code = 'operator'::text)))));

CREATE POLICY "Operadores ven todos los reportes" ON "public"."reports"
  FOR SELECT
  TO PUBLIC
  USING ((EXISTS ( SELECT 1
   FROM public.profiles
  WHERE ((profiles.id = auth.uid()) AND (profiles.role_code = 'operator'::text)))));

CREATE POLICY "Operadores leen y gestionan puntos de ruta" ON "public"."route_points"
  FOR SELECT
  TO PUBLIC
  USING (true);

CREATE POLICY "Cualquiera lee rutas activas" ON "public"."routes"
  FOR SELECT
  TO PUBLIC
  USING (((status = 'activa'::text) OR (EXISTS ( SELECT 1
   FROM public.profiles
  WHERE ((profiles.id = auth.uid()) AND (profiles.role_code = 'operator'::text))))));

CREATE POLICY "Lectura publica de capacidades de recoleccion" ON "public"."weekly_pickup_capacities"
  FOR SELECT
  TO PUBLIC
  USING (true);

COMMENT ON EXTENSION "postgis" IS 'PostGIS geometry and geography spatial types and functions';

GRANT EXECUTE ON FUNCTION "public"."enforce_bulky_request_status_transition"() TO PUBLIC, "anon", "authenticated";

REVOKE ALL ON FUNCTION "public"."enforce_bulky_request_status_transition"() FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."enforce_bulky_request_status_transition"() TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."enforce_bulky_request_status_transition"() TO "service_role";

GRANT EXECUTE ON FUNCTION "public"."enforce_report_status_transition"() TO PUBLIC, "anon", "authenticated";

REVOKE ALL ON FUNCTION "public"."enforce_report_status_transition"() FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."enforce_report_status_transition"() TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."enforce_report_status_transition"() TO "service_role";

GRANT EXECUTE ON FUNCTION "public"."get_ranked_collection_points"(public.geography) TO PUBLIC, "anon", "authenticated";

REVOKE ALL ON FUNCTION "public"."get_ranked_collection_points"(public.geography) FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."get_ranked_collection_points"(public.geography) TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."get_ranked_collection_points"(public.geography) TO "service_role";

GRANT EXECUTE ON FUNCTION "public"."redeem_coupon"(uuid) TO PUBLIC, "anon", "authenticated";

REVOKE ALL ON FUNCTION "public"."redeem_coupon"(uuid) FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."redeem_coupon"(uuid) TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."redeem_coupon"(uuid) TO "service_role";

GRANT EXECUTE ON FUNCTION "public"."sync_citizen_ecopoints_balance"() TO PUBLIC, "anon", "authenticated";

REVOKE ALL ON FUNCTION "public"."sync_citizen_ecopoints_balance"() FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."sync_citizen_ecopoints_balance"() TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."sync_citizen_ecopoints_balance"() TO "service_role";

GRANT EXECUTE ON FUNCTION "public"."update_updated_at_column"() TO PUBLIC, "anon", "authenticated";

REVOKE ALL ON FUNCTION "public"."update_updated_at_column"() FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."update_updated_at_column"() TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."update_updated_at_column"() TO "service_role";

GRANT EXECUTE ON FUNCTION "public"."validate_and_credit_pickup"(uuid, text, numeric, numeric, text) TO PUBLIC, "anon", "authenticated";

REVOKE ALL ON FUNCTION "public"."validate_and_credit_pickup"(uuid, text, numeric, numeric, text) FROM "postgres";

GRANT EXECUTE ON FUNCTION "public"."validate_and_credit_pickup"(uuid, text, numeric, numeric, text) TO "postgres";

GRANT EXECUTE ON FUNCTION "public"."validate_and_credit_pickup"(uuid, text, numeric, numeric, text) TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."bulky_waste_requests" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."bulky_waste_requests" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."bulky_waste_requests" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."bulky_waste_requests" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."businesses" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."businesses" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."businesses" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."businesses" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."citizens" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."citizens" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."citizens" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."citizens" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."collection_points" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."collection_points" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."collection_points" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."collection_points" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."companies" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."companies" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."companies" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."companies" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."coupon_redemptions" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."coupon_redemptions" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."coupon_redemptions" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."coupon_redemptions" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."coupons" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."coupons" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."coupons" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."coupons" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ecopoints_transactions" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."ecopoints_transactions" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ecopoints_transactions" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ecopoints_transactions" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."educational_completions" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."educational_completions" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."educational_completions" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."educational_completions" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."educational_content" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."educational_content" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."educational_content" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."educational_content" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."operators" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."operators" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."operators" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."operators" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."priority_weights" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."priority_weights" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."priority_weights" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."priority_weights" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."profiles" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."profiles" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."profiles" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."profiles" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recyclable_material_listings" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."recyclable_material_listings" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recyclable_material_listings" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recyclable_material_listings" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recycler_pickups" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."recycler_pickups" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recycler_pickups" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recycler_pickups" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recyclers" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."recyclers" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recyclers" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."recyclers" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."reports" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."reports" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."reports" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."reports" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."roles" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."roles" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."roles" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."roles" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."route_points" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."route_points" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."route_points" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."route_points" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."routes" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."routes" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."routes" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."routes" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."transaction_reasons" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."transaction_reasons" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."transaction_reasons" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."transaction_reasons" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."weekly_pickup_capacities" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."weekly_pickup_capacities" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."weekly_pickup_capacities" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."weekly_pickup_capacities" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."collection_points_priority_view" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."collection_points_priority_view" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."collection_points_priority_view" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."collection_points_priority_view" TO "service_role";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."neighborhood_recycling_ranking_view" TO "anon", "authenticated";

REVOKE ALL ON TABLE "public"."neighborhood_recycling_ranking_view" FROM "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."neighborhood_recycling_ranking_view" TO "postgres";

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."neighborhood_recycling_ranking_view" TO "service_role";

