-- EcoRuta Tuluá: Corrección de recursión infinita en políticas RLS cruzadas
-- Entre las tablas: companies, operators, recyclers y recyclable_material_listings
-- Solución: Funciones de chequeo con SECURITY DEFINER para aislar el contexto de privilegios.

-- 1. Funciones auxiliares de seguridad
CREATE OR REPLACE FUNCTION public.is_admin(user_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = user_id AND role_code = 'admin'
  );
$$;

CREATE OR REPLACE FUNCTION public.is_company_owner(c_id uuid, user_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT CASE 
    WHEN c_id IS NULL OR user_id IS NULL THEN false
    ELSE EXISTS (
      SELECT 1 FROM public.companies
      WHERE id = c_id AND owner_profile_id = user_id
    )
  END;
$$;

CREATE OR REPLACE FUNCTION public.is_company_operator(c_id uuid, user_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT CASE 
    WHEN c_id IS NULL OR user_id IS NULL THEN false
    ELSE EXISTS (
      SELECT 1 FROM public.operators
      WHERE company_id = c_id AND profile_id = user_id
    )
  END;
$$;

CREATE OR REPLACE FUNCTION public.is_approved_recycler(user_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.recyclers
    WHERE profile_id = user_id AND status = 'aprobado'
  );
$$;

CREATE OR REPLACE FUNCTION public.can_citizen_see_recycler(r_profile_id uuid, citizen_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.recyclable_material_listings l
    JOIN public.recycler_pickups p ON p.listing_id = l.id
    WHERE p.recycler_id = r_profile_id AND l.owner_id = citizen_id
  );
$$;

-- 2. Políticas RLS para companies (rompe ciclo con operators)
DROP POLICY IF EXISTS "Operadores leen su empresa asignada" ON public.companies;
CREATE POLICY "Operadores leen su empresa asignada" ON public.companies
  FOR SELECT TO authenticated
  USING (public.is_company_operator(companies.id, auth.uid()));

-- 3. Políticas RLS para operators (rompe ciclo con companies)
DROP POLICY IF EXISTS "Administrador EcoRuta y Administrador de Empresa gestionan operadores" ON public.operators;
CREATE POLICY "Administrador EcoRuta y Administrador de Empresa gestionan operadores" ON public.operators
  FOR ALL TO authenticated
  USING (
    public.is_admin(auth.uid())
    OR public.is_company_owner(operators.company_id, auth.uid())
  );

-- 4. Políticas RLS para recyclers (rompe ciclo con companies y recyclable_material_listings)
DROP POLICY IF EXISTS "Administrador de empresa y operadores gestionan recicladores vinculados" ON public.recyclers;
CREATE POLICY "Administrador de empresa y operadores gestionan recicladores vinculados" ON public.recyclers
  FOR ALL TO authenticated
  USING (
    public.is_company_owner(recyclers.company_id, auth.uid())
    OR public.is_company_operator(recyclers.company_id, auth.uid())
  );

DROP POLICY IF EXISTS "Ciudadanos ven reciclador asignado a su entrega" ON public.recyclers;
CREATE POLICY "Ciudadanos ven reciclador asignado a su entrega" ON public.recyclers
  FOR SELECT TO authenticated
  USING (public.can_citizen_see_recycler(recyclers.profile_id, auth.uid()));

-- 5. Políticas RLS para recyclable_material_listings (rompe ciclo con recyclers)
DROP POLICY IF EXISTS "Recicladores aprobados ven ofertas disponibles" ON public.recyclable_material_listings;
CREATE POLICY "Recicladores aprobados ven ofertas disponibles" ON public.recyclable_material_listings
  FOR SELECT TO PUBLIC
  USING (status = 'disponible' AND public.is_approved_recycler(auth.uid()));

DROP POLICY IF EXISTS "Recicladores aprobados reservan material disponible" ON public.recyclable_material_listings;
CREATE POLICY "Recicladores aprobados reservan material disponible" ON public.recyclable_material_listings
  FOR UPDATE TO authenticated
  USING (public.is_approved_recycler(auth.uid()));
