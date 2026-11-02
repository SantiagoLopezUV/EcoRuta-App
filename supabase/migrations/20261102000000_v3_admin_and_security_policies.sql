-- EcoRuta Tuluá — Migración v3: Alineación con IEEE 830, Panel Administrador EcoRuta y Políticas RLS
-- Resuelve vacíos de seguridad, agrega el rol 'company_admin', columnas de soporte y habilita RLS integral.

-- 1. CATÁLOGO DE ROLES (IEEE 830 RF-29, RF-30)
-- Actualización del Superadministrador de la plataforma municipal EcoRuta
UPDATE public.roles
SET name = 'Administrador EcoRuta',
    description = 'Superadministrador web: gestiona empresas prestadoras, verifica comercios y recicladores, y supervisa catálogos'
WHERE code = 'admin';

-- Creación del rol para el dueño / representante de la empresa gestora (aseo, reciclaje y/o escombros)
INSERT INTO public.roles (code, name, description, is_active)
VALUES (
  'company_admin', 
  'Administrador de Empresa', 
  'Administrador institucional de empresa gestora (aseo, reciclaje y/o escombros): administra equipo operativo, vinculación de recicladores y parámetros', 
  true
)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

-- Permitir lectura pública de roles para formularios de registro y selección de perfiles
DROP POLICY IF EXISTS "Lectura publica de roles" ON public.roles;
CREATE POLICY "Lectura publica de roles" ON public.roles
  FOR SELECT TO PUBLIC
  USING (true);

-- 2. MOTIVOS DE TRANSACCIÓN ECOPUNTOS (Lectura pública)
DROP POLICY IF EXISTS "Lectura publica de motivos de transaccion" ON public.transaction_reasons;
CREATE POLICY "Lectura publica de motivos de transaccion" ON public.transaction_reasons
  FOR SELECT TO PUBLIC
  USING (true);

-- 3. PERFILES DE USUARIO (Foto, Tipo de Documento y Teléfono - RF-31, RF-33)
ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS document_type text NOT NULL DEFAULT 'CC' CHECK (document_type IN ('CC', 'NIT', 'CE', 'PASAPORTE', 'TI')),
  ADD COLUMN IF NOT EXISTS avatar_url text,
  ADD COLUMN IF NOT EXISTS phone text;

-- 4. EMPRESAS GESTORAS (Aseo, Reciclaje, Escombros o Mixtas - RF-34, RF-35, RF-36)
-- La columna 'type' es un Array (text[]) que soporta: 'aseo', 'reciclaje', 'escombros' o combinaciones
ALTER TABLE public.companies
  ADD COLUMN IF NOT EXISTS owner_profile_id uuid REFERENCES public.profiles(id),
  ADD COLUMN IF NOT EXISTS status text NOT NULL DEFAULT 'aprobada' CHECK (status IN ('pendiente', 'aprobado', 'rechazada', 'suspendida')),
  ADD COLUMN IF NOT EXISTS rejection_reason text,
  ADD COLUMN IF NOT EXISTS rut_pdf_url text,
  ADD COLUMN IF NOT EXISTS legal_rep_name text,
  ADD COLUMN IF NOT EXISTS legal_rep_document_type text DEFAULT 'CC',
  ADD COLUMN IF NOT EXISTS legal_rep_document_id text,
  ADD COLUMN IF NOT EXISTS legal_rep_id_pdf_url text,
  ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS reviewed_by uuid REFERENCES public.profiles(id),
  ADD COLUMN IF NOT EXISTS reviewed_at timestamptz;

-- Políticas RLS para empresas
DROP POLICY IF EXISTS "Cualquiera lee empresas aprobadas y activas" ON public.companies;
CREATE POLICY "Cualquiera lee empresas aprobadas y activas" ON public.companies
  FOR SELECT TO PUBLIC
  USING (status = 'aprobado' AND is_active = true);

DROP POLICY IF EXISTS "Administrador EcoRuta gestiona todas las empresas" ON public.companies;
CREATE POLICY "Administrador EcoRuta gestiona todas las empresas" ON public.companies
  FOR ALL TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles 
      WHERE profiles.id = auth.uid() AND profiles.role_code = 'admin'
    )
  );

DROP POLICY IF EXISTS "Administrador de empresa gestiona su propia empresa" ON public.companies;
CREATE POLICY "Administrador de empresa gestiona su propia empresa" ON public.companies
  FOR ALL TO authenticated
  USING (owner_profile_id = auth.uid());

DROP POLICY IF EXISTS "Operadores leen su empresa asignada" ON public.companies;
CREATE POLICY "Operadores leen su empresa asignada" ON public.companies
  FOR SELECT TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.operators 
      WHERE operators.profile_id = auth.uid() AND operators.company_id = companies.id
    )
  );

DROP POLICY IF EXISTS "Empresas en autoregistro se insertan pendientes" ON public.companies;
CREATE POLICY "Empresas en autoregistro se insertan pendientes" ON public.companies
  FOR INSERT TO authenticated
  WITH CHECK (status = 'pendiente' AND owner_profile_id = auth.uid());

-- 5. COMERCIOS ALIADOS (RUT en PDF, Verificación y RLS - RF-05, RF-27, RF-37)
ALTER TABLE public.businesses
  ADD COLUMN IF NOT EXISTS rut_pdf_url text,
  ADD COLUMN IF NOT EXISTS rejection_reason text,
  ADD COLUMN IF NOT EXISTS reviewed_by uuid REFERENCES public.profiles(id),
  ADD COLUMN IF NOT EXISTS reviewed_at timestamptz;

DROP POLICY IF EXISTS "Comercios registran su perfil" ON public.businesses;
CREATE POLICY "Comercios registran su perfil" ON public.businesses
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = profile_id);

DROP POLICY IF EXISTS "Comercios gestionan su propio perfil" ON public.businesses;
CREATE POLICY "Comercios gestionan su propio perfil" ON public.businesses
  FOR ALL TO authenticated
  USING (auth.uid() = profile_id);

DROP POLICY IF EXISTS "Administrador EcoRuta gestiona todos los comercios" ON public.businesses;
CREATE POLICY "Administrador EcoRuta gestiona todos los comercios" ON public.businesses
  FOR ALL TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles 
      WHERE profiles.id = auth.uid() AND profiles.role_code = 'admin'
    )
  );

DROP POLICY IF EXISTS "Publico lee comercios aprobados" ON public.businesses;
CREATE POLICY "Publico lee comercios aprobados" ON public.businesses
  FOR SELECT TO PUBLIC
  USING (status = 'aprobado');

-- 6. RECICLADORES DE OFICIO (Cédula PDF, Foto y RLS - RF-06, RF-26, RF-39)
ALTER TABLE public.recyclers
  ADD COLUMN IF NOT EXISTS company_id uuid REFERENCES public.companies(id),
  ADD COLUMN IF NOT EXISTS id_document_pdf_url text,
  ADD COLUMN IF NOT EXISTS photo_url text,
  ADD COLUMN IF NOT EXISTS rejection_reason text,
  ADD COLUMN IF NOT EXISTS reviewed_by uuid REFERENCES public.profiles(id),
  ADD COLUMN IF NOT EXISTS reviewed_at timestamptz;

DROP POLICY IF EXISTS "Recicladores registran su perfil" ON public.recyclers;
CREATE POLICY "Recicladores registran su perfil" ON public.recyclers
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = profile_id);

DROP POLICY IF EXISTS "Recicladores gestionan su propio perfil" ON public.recyclers;
CREATE POLICY "Recicladores gestionan su propio perfil" ON public.recyclers
  FOR ALL TO authenticated
  USING (auth.uid() = profile_id);

DROP POLICY IF EXISTS "Administrador EcoRuta gestiona todos los recicladores" ON public.recyclers;
CREATE POLICY "Administrador EcoRuta gestiona todos los recicladores" ON public.recyclers
  FOR ALL TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles 
      WHERE profiles.id = auth.uid() AND profiles.role_code = 'admin'
    )
  );

DROP POLICY IF EXISTS "Administrador de empresa y operadores gestionan recicladores vinculados" ON public.recyclers;
CREATE POLICY "Administrador de empresa y operadores gestionan recicladores vinculados" ON public.recyclers
  FOR ALL TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.companies 
      WHERE companies.id = recyclers.company_id AND companies.owner_profile_id = auth.uid()
    )
    OR EXISTS (
      SELECT 1 FROM public.operators 
      WHERE operators.profile_id = auth.uid() AND operators.company_id = recyclers.company_id
    )
  );

DROP POLICY IF EXISTS "Ciudadanos ven reciclador asignado a su entrega" ON public.recyclers;
CREATE POLICY "Ciudadanos ven reciclador asignado a su entrega" ON public.recyclers
  FOR SELECT TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.recyclable_material_listings l
      JOIN public.recycler_pickups p ON p.listing_id = l.id
      WHERE l.owner_id = auth.uid() AND p.recycler_id = recyclers.profile_id
    )
  );

-- 7. CIUDADANOS (RLS - RF-01, RF-03)
DROP POLICY IF EXISTS "Ciudadanos registran su perfil" ON public.citizens;
CREATE POLICY "Ciudadanos registran su perfil" ON public.citizens
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = profile_id);

DROP POLICY IF EXISTS "Ciudadanos gestionan su propio perfil" ON public.citizens;
CREATE POLICY "Ciudadanos gestionan su propio perfil" ON public.citizens
  FOR ALL TO authenticated
  USING (auth.uid() = profile_id);

DROP POLICY IF EXISTS "Personal autorizado consulta ciudadanos" ON public.citizens;
CREATE POLICY "Personal autorizado consulta ciudadanos" ON public.citizens
  FOR SELECT TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles 
      WHERE profiles.id = auth.uid() AND profiles.role_code IN ('operator', 'company_admin', 'admin')
    )
  );

-- 8. OPERADORES (Despachadores y Choferes - RF-38)
ALTER TABLE public.operators
  ADD COLUMN IF NOT EXISTS vehicle_plate text;

DROP POLICY IF EXISTS "Operadores leen su propio registro" ON public.operators;
CREATE POLICY "Operadores leen su propio registro" ON public.operators
  FOR SELECT TO authenticated
  USING (auth.uid() = profile_id);

DROP POLICY IF EXISTS "Administrador EcoRuta y Administrador de Empresa gestionan operadores" ON public.operators;
CREATE POLICY "Administrador EcoRuta y Administrador de Empresa gestionan operadores" ON public.operators
  FOR ALL TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles 
      WHERE profiles.id = auth.uid() AND profiles.role_code = 'admin'
    )
    OR EXISTS (
      SELECT 1 FROM public.companies 
      WHERE companies.id = operators.company_id AND companies.owner_profile_id = auth.uid()
    )
  );

-- 9. ESCOMBROS Y VOLUMINOSOS (RCD - RF-13, RF-14, RF-46)
DROP POLICY IF EXISTS "Ciudadanos crean solicitudes de escombros" ON public.bulky_waste_requests;
CREATE POLICY "Ciudadanos crean solicitudes de escombros" ON public.bulky_waste_requests
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = citizen_id);

DROP POLICY IF EXISTS "Ciudadanos ven sus propias solicitudes de escombros" ON public.bulky_waste_requests;
CREATE POLICY "Ciudadanos ven sus propias solicitudes de escombros" ON public.bulky_waste_requests
  FOR SELECT TO authenticated
  USING (auth.uid() = citizen_id);

DROP POLICY IF EXISTS "Ciudadanos cancelan sus solicitudes pendientes" ON public.bulky_waste_requests;
CREATE POLICY "Ciudadanos cancelan sus solicitudes pendientes" ON public.bulky_waste_requests
  FOR UPDATE TO authenticated
  USING (auth.uid() = citizen_id AND status = 'pendiente')
  WITH CHECK (auth.uid() = citizen_id AND status = 'cancelado');

DROP POLICY IF EXISTS "Empresas y Operadores gestionan solicitudes de escombros" ON public.bulky_waste_requests;
CREATE POLICY "Empresas y Operadores gestionan solicitudes de escombros" ON public.bulky_waste_requests
  FOR ALL TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.companies 
      WHERE (bulky_waste_requests.company_id IS NULL OR bulky_waste_requests.company_id = companies.id)
        AND companies.owner_profile_id = auth.uid()
    )
    OR EXISTS (
      SELECT 1 FROM public.operators 
      WHERE operators.profile_id = auth.uid() 
        AND (bulky_waste_requests.company_id IS NULL OR bulky_waste_requests.company_id = operators.company_id)
    )
    OR EXISTS (
      SELECT 1 FROM public.profiles 
      WHERE profiles.id = auth.uid() AND profiles.role_code = 'admin'
    )
  );

-- 10. PUBLICACIONES DE MATERIAL APROVECHABLE (RF-16, RF-18)
DROP POLICY IF EXISTS "Ciudadanos publican material aprovechable" ON public.recyclable_material_listings;
CREATE POLICY "Ciudadanos publican material aprovechable" ON public.recyclable_material_listings
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = owner_id);

DROP POLICY IF EXISTS "Ciudadanos gestionan sus propias publicaciones" ON public.recyclable_material_listings;
CREATE POLICY "Ciudadanos gestionan sus propias publicaciones" ON public.recyclable_material_listings
  FOR ALL TO authenticated
  USING (auth.uid() = owner_id);

DROP POLICY IF EXISTS "Recicladores aprobados reservan material disponible" ON public.recyclable_material_listings;
CREATE POLICY "Recicladores aprobados reservan material disponible" ON public.recyclable_material_listings
  FOR UPDATE TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.recyclers 
      WHERE recyclers.profile_id = auth.uid() AND recyclers.status = 'aprobado'
    )
  );

-- 11. ORDENES DE RECOLECCIÓN DE RECICLAJE (RF-18, RF-19)
DROP POLICY IF EXISTS "Recicladores ven sus ordenes de recoleccion" ON public.recycler_pickups;
CREATE POLICY "Recicladores ven sus ordenes de recoleccion" ON public.recycler_pickups
  FOR SELECT TO authenticated
  USING (auth.uid() = recycler_id);

DROP POLICY IF EXISTS "Ciudadanos ven recolecciones de sus ofertas" ON public.recycler_pickups;
CREATE POLICY "Ciudadanos ven recolecciones de sus ofertas" ON public.recycler_pickups
  FOR SELECT TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.recyclable_material_listings l
      WHERE l.id = recycler_pickups.listing_id AND l.owner_id = auth.uid()
    )
  );

-- 12. RUTAS: ASIGNACIÓN A CHOFER O RECICLADOR VINCULADO (RF-43, RF-45, RF-51)
ALTER TABLE public.routes
  ADD COLUMN IF NOT EXISTS assigned_driver_id uuid REFERENCES public.operators(profile_id),
  ADD COLUMN IF NOT EXISTS assigned_recycler_id uuid REFERENCES public.recyclers(profile_id),
  ADD COLUMN IF NOT EXISTS published_at timestamptz;

DROP POLICY IF EXISTS "Choferes y recicladores leen sus rutas asignadas" ON public.routes;
CREATE POLICY "Choferes y recicladores leen sus rutas asignadas" ON public.routes
  FOR SELECT TO authenticated
  USING (
    assigned_driver_id = auth.uid() 
    OR assigned_recycler_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM public.operators 
      WHERE operators.profile_id = auth.uid() AND operators.company_id = routes.company_id
    )
    OR EXISTS (
      SELECT 1 FROM public.companies 
      WHERE companies.id = routes.company_id AND companies.owner_profile_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS "Despachadores y administradores de empresa gestionan rutas" ON public.routes;
CREATE POLICY "Despachadores y administradores de empresa gestionan rutas" ON public.routes
  FOR ALL TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.operators 
      WHERE operators.profile_id = auth.uid() AND operators.company_id = routes.company_id
    )
    OR EXISTS (
      SELECT 1 FROM public.companies 
      WHERE companies.id = routes.company_id AND companies.owner_profile_id = auth.uid()
    )
  );

-- 13. STORAGE BUCKETS (Avatares y Documentos PDF - RNF-06)
INSERT INTO storage.buckets (id, name, public)
VALUES 
  ('avatars', 'avatars', true),
  ('institutional-docs', 'institutional-docs', false)
ON CONFLICT (id) DO NOTHING;

-- Políticas de Storage para Avatares
DROP POLICY IF EXISTS "Lectura publica de avatares" ON storage.objects;
CREATE POLICY "Lectura publica de avatares" ON storage.objects
  FOR SELECT TO PUBLIC
  USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "Usuarios suben su propio avatar" ON storage.objects;
CREATE POLICY "Usuarios suben su propio avatar" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "Usuarios actualizan su propio avatar" ON storage.objects;
CREATE POLICY "Usuarios actualizan su propio avatar" ON storage.objects
  FOR UPDATE TO authenticated
  USING (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);

-- Políticas de Storage para Documentos Institucionales (RUTs y Cédulas privadas)
DROP POLICY IF EXISTS "Usuarios suben sus propios documentos de soporte" ON storage.objects;
CREATE POLICY "Usuarios suben sus propios documentos de soporte" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'institutional-docs' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "Usuarios y Administrador leen documentos de soporte" ON storage.objects;
CREATE POLICY "Usuarios y Administrador leen documentos de soporte" ON storage.objects
  FOR SELECT TO authenticated
  USING (
    bucket_id = 'institutional-docs' AND (
      (storage.foldername(name))[1] = auth.uid()::text
      OR EXISTS (
        SELECT 1 FROM public.profiles 
        WHERE profiles.id = auth.uid() AND profiles.role_code = 'admin'
      )
    )
  );
