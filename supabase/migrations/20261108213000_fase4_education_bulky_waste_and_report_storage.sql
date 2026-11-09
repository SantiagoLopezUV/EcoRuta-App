-- Migración Fase 4: Educación Ambiental, Escombros/RCD y Almacenamiento de Fotos de Reportes
-- Implementa contenido  de educación, RPC de acreditación de EcoPuntos,
-- Gestores autorizados RCD y el bucket de Storage report-photos

-- 1. CREACIÓN DEL BUCKET report-photos EN SUPABASE STORAGE
INSERT INTO storage.buckets (id, name, public)
VALUES ('report-photos', 'report-photos', true)
ON CONFLICT (id) DO UPDATE SET public = true;

DROP POLICY IF EXISTS "Lectura publica de fotos de reportes" ON storage.objects;
CREATE POLICY "Lectura publica de fotos de reportes" ON storage.objects
  FOR SELECT
  USING (bucket_id = 'report-photos');

DROP POLICY IF EXISTS "Ciudadanos suben fotos de sus reportes" ON storage.objects;
CREATE POLICY "Ciudadanos suben fotos de sus reportes" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (
    bucket_id = 'report-photos' 
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

-- AMPLIACIÓN Y SEMILLAS DE CONTENIDO EDUCATIVO
ALTER TABLE public.educational_content ADD COLUMN IF NOT EXISTS summary text;
ALTER TABLE public.educational_content ADD COLUMN IF NOT EXISTS body text;
ALTER TABLE public.educational_content ADD COLUMN IF NOT EXISTS icon text;

INSERT INTO public.educational_content (id, title, content_type, points_reward, summary, body, icon, is_active)
VALUES
  (
    '00000000-0000-0000-0000-000000000101',
    'Cómo separar tus residuos',
    'separacion_residuos',
    20,
    'Guía rápida del código de colores en Colombia (blanco, negro y verde)',
    'En Colombia rige el código unificado de colores para la separación en la fuente:\n\n• Blanco: Plásticos, vidrio, metales, papel y cartón limpios y secos.\n• Negro: Papel higiénico, servilletas, papeles metalizados y residuos no aprovechables.\n• Verde: Restos de comida y desechos agrícolas orgánicos.',
    'list',
    true
  ),
  (
    '00000000-0000-0000-0000-000000000102',
    'Puntos Limpios en Tuluá',
    'puntos_limpios',
    20,
    'Conoce los contenedores públicos para depositar materiales aprovechables',
    'Los Puntos Limpios son lugares estratégicos en parques y avenidas de Tuluá donde puedes depositar botellas plásticas, latas y cartón sin esperar la ruta del camión de aseo ni de recolección.',
    'recycling',
    true
  ),
  (
    '00000000-0000-0000-0000-000000000103',
    'Manejo de Escombros y Voluminosos (RCD)',
    'escombros_rcd',
    20,
    'Directrices oficiales para entrega de muebles, colchones y escombros de obra',
    'Los escombros y muebles viejos NO deben dejarse en esquinas ni andenes. En Tuluá debes agendar la recolección especial con gestores autorizados o llevarlos a la escombrera municipal.',
    'debris',
    true
  ),
  (
    '00000000-0000-0000-0000-000000000104',
    'Economía Circular y EcoPuntos',
    'ecopuntos_beneficios',
    20,
    'Aprende cómo ganar y redimir EcoPuntos en comercios aliados de Tuluá',
    'Al entregar materiales aprovechables a recicladores de oficio o completar guías ambientales, acumulas EcoPuntos para canjear por descuentos en comercios locales participantes.',
    'ecopoints',
    true
  )
ON CONFLICT (id) DO UPDATE SET
  title = EXCLUDED.title,
  content_type = EXCLUDED.content_type,
  points_reward = EXCLUDED.points_reward,
  summary = EXCLUDED.summary,
  body = EXCLUDED.body,
  icon = EXCLUDED.icon,
  is_active = EXCLUDED.is_active;

-- FUNCIÓN RPC PARA REGISTRAR FINALIZACIÓN DE LECCIÓN Y ACREDITAR ECOPUNTOS
CREATE OR REPLACE FUNCTION public.complete_educational_lesson(p_content_id uuid)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'auth'
AS $function$
DECLARE
  v_user_id uuid := auth.uid();
  v_reward integer;
  v_completion_id uuid;
BEGIN
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'Usuario no autenticado.';
  END IF;

  --Verifica que la lección exista y esté activa
  SELECT COALESCE(points_reward, 20) INTO v_reward
  FROM public.educational_content
  WHERE id = p_content_id AND is_active = true;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'La lección educativa no existe o está inactiva.';
  END IF;

  --Verifica si el usuario ya la completó previamente
  IF EXISTS (
    SELECT 1 FROM public.educational_completions
    WHERE citizen_id = v_user_id AND content_id = p_content_id
  ) THEN
    RETURN 0;
  END IF;

  --Inserta el registro de completitud
  INSERT INTO public.educational_completions (citizen_id, content_id)
  VALUES (v_user_id, p_content_id)
  RETURNING id INTO v_completion_id;

  --Registra la transacción de EcoPuntos
  INSERT INTO public.ecopoints_transactions (
    citizen_id,
    amount,
    reason_code,
    completion_id
  ) VALUES (
    v_user_id,
    v_reward,
    'capacitacion',
    v_completion_id
  );

  RETURN v_reward;
END;
$function$;

GRANT EXECUTE ON FUNCTION public.complete_educational_lesson(uuid) TO authenticated;

-- TABLA Y SEMILLAS DE GESTORES AUTORIZADOS DE ESCOMBROS Y RCD
CREATE TABLE IF NOT EXISTS public.authorized_rcd_managers (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    name text NOT NULL,
    item_types text[] DEFAULT ARRAY['escombros'::text] NOT NULL,
    phone text,
    address text,
    operating_hours text,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.authorized_rcd_managers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Lectura publica de gestores autorizados RCD" ON public.authorized_rcd_managers;
CREATE POLICY "Lectura publica de gestores autorizados RCD"
  ON public.authorized_rcd_managers
  FOR SELECT
  USING (is_active = true);

GRANT SELECT ON public.authorized_rcd_managers TO anon, authenticated;

INSERT INTO public.authorized_rcd_managers (name, item_types, phone, address, operating_hours, description, is_active)
VALUES
  (
    'SEDAMA / Veolia Aseo Tuluá',
    ARRAY['muebles', 'colchones', 'escombros'],
    '602 225 5555',
    'Carrera 28 # 25-40, Centro, Tuluá',
    'Lunes a Viernes 7:30 AM - 5:00 PM',
    'Servicio municipal de recolección de voluminosos y escombros domiciliarios hasta 1 m³.',
    true
  ),
  (
    'Escombrera Municipal Autorizada',
    ARRAY['escombros'],
    '602 224 1000',
    'Vía Tuluá - Riofrío Km 3',
    'Lunes a Sábado 7:00 AM - 4:00 PM',
    'Punto oficial de disposición final y aprovechamiento de residuos de construcción y demolición.',
    true
  ),
  (
    'Gestores del Valle - Muebles y Colchones',
    ARRAY['muebles', 'colchones'],
    '315 800 2020',
    'Zona Industrial, Tuluá',
    'Lunes a Viernes 8:00 AM - 4:30 PM',
    'Recuperación de espuma, resortes metálicos y madera de muebles en desuso.',
    true
  )
ON CONFLICT DO NOTHING;
