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
