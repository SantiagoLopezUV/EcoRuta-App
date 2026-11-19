-- Garantiza la columna de telefono en ciudadanos
ALTER TABLE public.citizens
  ADD COLUMN IF NOT EXISTS phone text;

-- Crea automáticamente el perfil y la entidad correspondiente al registrar un usuario en auth.users
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'auth'
AS $$
DECLARE
  v_role_code text;
  v_full_name text;
  v_document_id text;
  v_phone text;
  v_neighborhood text;
  v_business_name text;
  v_category text;
  v_address text;
  v_rut_url text;
  v_association text;
  v_company_id text;
  v_id_doc_url text;
BEGIN
  v_role_code := COALESCE(NULLIF(TRIM(NEW.raw_user_meta_data->>'role_code'), ''), 'citizen');
  v_full_name := COALESCE(
    NULLIF(TRIM(NEW.raw_user_meta_data->>'full_name'), ''),
    NULLIF(TRIM(NEW.raw_user_meta_data->>'name'), ''),
    split_part(NEW.email, '@', 1)
  );
  v_document_id := NULLIF(TRIM(NEW.raw_user_meta_data->>'document_id'), '');
  v_phone := NULLIF(TRIM(NEW.raw_user_meta_data->>'phone'), '');

  -- Inserta o actualiza el perfil base sin restricción de RLS
  INSERT INTO public.profiles (
    id,
    document_id,
    role_code,
    full_name,
    email,
    phone,
    must_change_password
  ) VALUES (
    NEW.id,
    v_document_id,
    v_role_code,
    v_full_name,
    NEW.email,
    v_phone,
    COALESCE((NEW.raw_user_meta_data->>'must_change_password')::boolean, false)
  )
  ON CONFLICT (id) DO UPDATE SET
    document_id = COALESCE(EXCLUDED.document_id, public.profiles.document_id),
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    phone = COALESCE(EXCLUDED.phone, public.profiles.phone);

  -- Inserta registro en la tabla de rol correspondiente
  IF v_role_code = 'citizen' THEN
    v_neighborhood := NULLIF(TRIM(NEW.raw_user_meta_data->>'neighborhood'), '');

    INSERT INTO public.citizens (
      profile_id,
      phone,
      neighborhood
    ) VALUES (
      NEW.id,
      v_phone,
      v_neighborhood
    )
    ON CONFLICT (profile_id) DO UPDATE SET
      phone = COALESCE(EXCLUDED.phone, public.citizens.phone),
      neighborhood = COALESCE(EXCLUDED.neighborhood, public.citizens.neighborhood);

  ELSIF v_role_code = 'recycler' THEN
    v_phone := NULLIF(TRIM(NEW.raw_user_meta_data->>'phone'), '');
    v_association := NULLIF(TRIM(NEW.raw_user_meta_data->>'association_name'), '');
    v_company_id := NULLIF(TRIM(NEW.raw_user_meta_data->>'company_id'), '');
    v_id_doc_url := NULLIF(TRIM(NEW.raw_user_meta_data->>'id_document_pdf_url'), '');

    INSERT INTO public.recyclers (
      profile_id,
      phone,
      association_name,
      company_id,
      id_document_pdf_url,
      status
    ) VALUES (
      NEW.id,
      v_phone,
      v_association,
      CASE WHEN v_company_id IS NOT NULL THEN v_company_id::uuid ELSE NULL END,
      v_id_doc_url,
      'pendiente'
    )
    ON CONFLICT (profile_id) DO UPDATE SET
      phone = COALESCE(EXCLUDED.phone, public.recyclers.phone),
      association_name = COALESCE(EXCLUDED.association_name, public.recyclers.association_name),
      company_id = COALESCE(EXCLUDED.company_id, public.recyclers.company_id),
      id_document_pdf_url = COALESCE(EXCLUDED.id_document_pdf_url, public.recyclers.id_document_pdf_url);

  ELSIF v_role_code = 'business' THEN
    v_business_name := COALESCE(NULLIF(TRIM(NEW.raw_user_meta_data->>'business_name'), ''), v_full_name);
    v_category := NULLIF(TRIM(NEW.raw_user_meta_data->>'category'), '');
    v_address := NULLIF(TRIM(NEW.raw_user_meta_data->>'address'), '');
    v_rut_url := NULLIF(TRIM(NEW.raw_user_meta_data->>'rut_pdf_url'), '');

    INSERT INTO public.businesses (
      profile_id,
      business_name,
      category,
      address,
      rut_pdf_url,
      status
    ) VALUES (
      NEW.id,
      v_business_name,
      v_category,
      v_address,
      v_rut_url,
      'pendiente'
    )
    ON CONFLICT (profile_id) DO UPDATE SET
      business_name = COALESCE(EXCLUDED.business_name, public.businesses.business_name),
      category = COALESCE(EXCLUDED.category, public.businesses.category),
      address = COALESCE(EXCLUDED.address, public.businesses.address),
      rut_pdf_url = COALESCE(EXCLUDED.rut_pdf_url, public.businesses.rut_pdf_url);
  END IF;

  RETURN NEW;
END;
$$;

-- Disparador en auth.users tras registrar una nueva cuenta
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user();
