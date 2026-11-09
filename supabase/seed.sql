-- Datos para los catálogos base de EcoRuta Tuluá
-- Carga los roles del sistema y los motivos de transacción de EcoPuntos al reiniciar la base de datos local

-- Registra los roles permitidos por las llaves foráneas de perfiles y asignación multi-rol
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

-- Registra los motivos de acreditación y consumo requeridos por las transacciones de EcoPuntos
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


-- Carga inicial de contenidos pedagógicos de reciclaje y educación ambiental (ECO-953)
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

-- Gestores autorizados para residuos de manejo especial y escombros RCD (ECO-1011)
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
