-- Tablas de catalogo geografico y jerarquia territorial

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.prevent_administrative_unit_cycles()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  cycle_exists boolean;
BEGIN
  IF NEW.parent_id IS NULL THEN
    RETURN NEW;
  END IF;

  WITH RECURSIVE ancestors(id, parent_id) AS (
    SELECT id, parent_id
    FROM public.administrative_units
    WHERE id = NEW.parent_id

    UNION

    SELECT parent.id, parent.parent_id
    FROM public.administrative_units parent
    INNER JOIN ancestors child
      ON parent.id = child.parent_id
  )
  SELECT EXISTS (
    SELECT 1
    FROM ancestors
    WHERE id = NEW.id
  )
  INTO cycle_exists;

  IF cycle_exists THEN
    RAISE EXCEPTION 'No se permite crear ciclos en la jerarquia administrativa.';
  END IF;

  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.sync_administrative_unit_country()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO 'pg_catalog', 'public'
AS $$
DECLARE
  parent_country_id uuid;
BEGIN
  IF NEW.parent_id IS NULL THEN
    IF NEW.country_id IS NULL THEN
      RAISE EXCEPTION 'Una unidad administrativa raiz debe tener country_id';
    END IF;
  ELSE
    SELECT au.country_id
    INTO parent_country_id
    FROM public.administrative_units AS au
    WHERE au.id = NEW.parent_id;

    IF NOT FOUND THEN
      RAISE EXCEPTION 'No existe la unidad administrativa padre con ID %', NEW.parent_id;
    END IF;

    NEW.country_id := parent_country_id;
  END IF;

  RETURN NEW;
END;
$$;

CREATE TABLE IF NOT EXISTS public.countries (
  id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  name text DEFAULT ''::text NOT NULL UNIQUE,
  iso_code_alpha_2 character varying DEFAULT ''::character varying NOT NULL UNIQUE,
  iso_code_alpha_3 character varying DEFAULT ''::character varying NOT NULL UNIQUE,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  CONSTRAINT countries_iso_code_alpha_2_check CHECK (length(iso_code_alpha_2::text) = 2),
  CONSTRAINT countries_iso_code_alpha_3_check CHECK (length(iso_code_alpha_3::text) = 3)
);

CREATE TABLE IF NOT EXISTS public.administrative_units (
  id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  country_id uuid NOT NULL,
  parent_id uuid,
  name text NOT NULL,
  unit_type text NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  CONSTRAINT administrative_units_id_country_unique UNIQUE (id, country_id),
  CONSTRAINT administrative_units_name_not_empty CHECK (length(btrim(name)) > 0),
  CONSTRAINT administrative_units_no_self_parent CHECK (parent_id IS DISTINCT FROM id),
  CONSTRAINT administrative_units_type_normalized CHECK (unit_type = lower(btrim(unit_type))),
  CONSTRAINT administrative_units_type_not_empty CHECK (length(btrim(unit_type)) > 0),
  CONSTRAINT administrative_units_country_id_fkey FOREIGN KEY (country_id) REFERENCES public.countries(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT administrative_units_parent_country_fkey FOREIGN KEY (parent_id, country_id) REFERENCES public.administrative_units(id, country_id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS public.local_areas (
  id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  administrative_unit_id uuid NOT NULL,
  name text DEFAULT ''::text NOT NULL,
  area_type text DEFAULT ''::text NOT NULL,
  official_code text,
  is_active boolean DEFAULT true NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  CONSTRAINT local_areas_name_not_empty CHECK (length(btrim(name)) > 0),
  CONSTRAINT local_areas_official_code_not_empty CHECK ((official_code IS NULL) OR (length(btrim(official_code)) > 0)),
  CONSTRAINT local_areas_type_normalized CHECK (area_type = lower(btrim(area_type))),
  CONSTRAINT local_areas_type_not_empty CHECK (length(btrim(area_type)) > 0),
  CONSTRAINT local_areas_administrative_unit_id_fkey FOREIGN KEY (administrative_unit_id) REFERENCES public.administrative_units(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS administrative_units_unique_children
  ON public.administrative_units USING btree (country_id, parent_id, unit_type, lower(btrim(name)))
  WHERE (parent_id IS NOT NULL);

CREATE UNIQUE INDEX IF NOT EXISTS administrative_units_unique_roots
  ON public.administrative_units USING btree (country_id, unit_type, lower(btrim(name)))
  WHERE (parent_id IS NULL);

CREATE INDEX IF NOT EXISTS idx_administrative_units_country
  ON public.administrative_units USING btree (country_id);

CREATE INDEX IF NOT EXISTS idx_administrative_units_parent
  ON public.administrative_units USING btree (parent_id);

CREATE INDEX IF NOT EXISTS idx_local_areas_active
  ON public.local_areas USING btree (administrative_unit_id)
  WHERE (is_active = true);

CREATE INDEX IF NOT EXISTS idx_local_areas_administrative_unit
  ON public.local_areas USING btree (administrative_unit_id);

CREATE UNIQUE INDEX IF NOT EXISTS local_areas_unique_name
  ON public.local_areas USING btree (administrative_unit_id, area_type, lower(btrim(name)));

DROP TRIGGER IF EXISTS trg_administrative_units_prevent_cycles ON public.administrative_units;
CREATE TRIGGER trg_administrative_units_prevent_cycles
  BEFORE INSERT OR UPDATE OF parent_id ON public.administrative_units
  FOR EACH ROW EXECUTE FUNCTION public.prevent_administrative_unit_cycles();

DROP TRIGGER IF EXISTS trg_administrative_units_updated_at ON public.administrative_units;
CREATE TRIGGER trg_administrative_units_updated_at
  BEFORE UPDATE ON public.administrative_units
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS trg_sync_administrative_unit_country ON public.administrative_units;
CREATE TRIGGER trg_sync_administrative_unit_country
  BEFORE INSERT OR UPDATE OF parent_id, country_id ON public.administrative_units
  FOR EACH ROW EXECUTE FUNCTION public.sync_administrative_unit_country();

DROP TRIGGER IF EXISTS trg_local_areas_updated_at ON public.local_areas;
CREATE TRIGGER trg_local_areas_updated_at
  BEFORE UPDATE ON public.local_areas
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- Vinculacion con la tabla ciudadanos
ALTER TABLE public.citizens
  ADD COLUMN IF NOT EXISTS local_area_id uuid;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'citizens_local_area_id_fkey'
  ) THEN
    ALTER TABLE public.citizens
      ADD CONSTRAINT citizens_local_area_id_fkey
      FOREIGN KEY (local_area_id) REFERENCES public.local_areas(id)
      ON UPDATE CASCADE ON DELETE RESTRICT;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_citizens_local_area_id
  ON public.citizens USING btree (local_area_id);

-- Politicas de seguridad
ALTER TABLE public.countries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.administrative_units ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.local_areas ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE tablename = 'countries' AND policyname = 'Anyone can read countries'
  ) THEN
    CREATE POLICY "Anyone can read countries"
      ON public.countries FOR SELECT
      TO authenticated, anon
      USING (true);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE tablename = 'administrative_units' AND policyname = 'Anyone can read administrative units'
  ) THEN
    CREATE POLICY "Anyone can read administrative units"
      ON public.administrative_units FOR SELECT
      TO authenticated, anon
      USING (true);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE tablename = 'local_areas' AND policyname = 'Anyone can read active local areas'
  ) THEN
    CREATE POLICY "Anyone can read active local areas"
      ON public.local_areas FOR SELECT
      TO authenticated, anon
      USING (is_active = true);
  END IF;
END $$;

-- Permisos de lectura
GRANT ALL ON TABLE public.countries TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.administrative_units TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.local_areas TO anon, authenticated, service_role;
GRANT ALL ON FUNCTION public.set_updated_at() TO anon, authenticated, service_role;
