# Entorno Backend Local con Supabase CLI y Docker - EcoRuta

Este directorio (`supabase/`) contiene la configuración y los artefactos de infraestructura como código (IaC) necesarios para ejecutar el backend de EcoRuta de manera local e independiente mediante contenedores Docker y la interfaz de línea de comandos de Supabase (Supabase CLI).

Su propósito es proveer a la aplicación móvil (`app/`) un entorno Backend-as-a-Service (BaaS) autosuficiente, permitiendo el desarrollo y la ejecución de pruebas de integración sin depender de conexión a internet ni consumir cuotas de servicios en la nube.

---

## 1. Componentes del Entorno Contenerizado

Al ejecutar el comando de inicio (`supabase start`), el motor de Docker despliega una serie de microservicios contenerizados que replican la arquitectura del backend de producción:

| Servicio | Puerto Local | Descripción y Rol en EcoRuta |
| :--- | :--- | :--- |
| **PostgreSQL + PostGIS** | `54322` | Base de datos relacional con extensión espacial activa para el cálculo de geocercas (< 100 m) y mapas de calor. |
| **Supabase Studio** | `54323` | Interfaz web gráfica para visualizar tablas, registros, logs y ejecutar sentencias SQL en el navegador (`http://localhost:54323`). |
| **API Gateway / Kong** | `54321` | Punto de entrada unificado para todas las solicitudes REST, GraphQL y almacenamiento. |
| **Supabase Auth (GoTrue)** | `54321/auth/v1` | Servicio emisor y validador de tokens JWT para ciudadanos, choferes y recicladores. |
| **Supabase Storage** | `54321/storage/v1` | Servidor compatible con S3 para almacenamiento local de fotografías de reportes comunitarios y documentos PDF. |
| **Servidor SMTP (Inbucket)** | `54324` | Capturador de correos electrónicos de prueba en entorno local (`http://localhost:54324`) para verificar contraseñas temporales sin envíos reales. |

---

## 2. Articulación con la Arquitectura Móvil (Android)

La aplicación móvil se conecta con este entorno mediante el cliente configurado en:  
`app/src/main/java/com/ecoruta/app/core/network/supabase/SupabaseClientProvider.kt`

### 2.1 Enrutamiento de Red desde el Emulador

Dado que el emulador de Android Studio corre dentro de una máquina virtual aislada, no puede resolver la dirección `localhost` o `127.0.0.1` hacia la máquina anfitriona:

* **Dirección para Emulador Android:** `http://10.0.2.2:54321`
* **Dirección para Dispositivo Físico:** `http://<IP_LOCAL_DE_TU_PC>:54321`

Cualquier petición enviada desde los repositorios de la capa de datos (`data/repository/`) es interceptada por el API Gateway local en el puerto `54321` y dirigida al contenedor correspondiente de Docker.

---

## 3. Estructura y Ciclo de Vida de Datos

Dentro de este directorio se gestionan las modificaciones a la base de datos bajo control de versiones:

* **`config.toml`:** Archivo principal de configuración del CLI donde se definen puertos, límites de carga, expiración de tokens y parámetros de almacenamiento.
* **`migrations/`:** Directorio donde se almacenan cronológicamente los scripts de definición de esquema (`.sql`) para tablas, funciones almacenadas (RPC) y políticas de seguridad RLS:
  - `20261025055033_remote_schema.sql`: Esquema base v1 sincronizado desde Supabase Cloud.
  - `20261025063000_v2_changes.sql`: Alineación normativa IEEE 830, sub-roles en español (`administrador_empresa`, `despachador`, `chofer`), ciclo canónico de rutas (`borrador`, `publicada`, `en_curso`, `finalizada`, `cancelada`), pesaje ponderado (30%, 30%, 25%, 15%), validación de cupones, evidencia fotográfica múltiple, radar de alerta desacoplado (`radar_subscriptions`), soporte multi-rol dinámico y políticas RLS para Modo Visitante.
* **`seed.sql`:** Script de inicialización ejecutado automáticamente tras un reinicio de base de datos para poblar el sistema con datos de prueba (lecciones de educación ambiental, barrios de Tuluá y usuarios base).
* **`functions/`:** Directorio para el código fuente de Edge Functions ejecutadas bajo el runtime Deno.

---

## 4. Comandos de Gestión Operativa y Sincronización

Los siguientes comandos se ejecutan desde la raíz del proyecto mediante la terminal:

### 4.1 Gestión del Ciclo Local

| Comando | Propósito |
| :--- | :--- |
| `supabase start` | Inicia todos los contenedores Docker locales del backend. |
| `supabase stop` | Detiene los contenedores y libera los puertos del sistema. |
| `supabase status` | Muestra el estado de ejecución, endpoints y claves de acceso (`anon key`, `service_role key`). |
| `supabase db reset` | Recrea la base de datos local desde cero, aplicando todas las migraciones en orden y ejecutando `seed.sql`. |
| `supabase db diff -f nombre_cambio` | Compara el estado actual de la base de datos local con los archivos de migración y genera un nuevo archivo SQL con las diferencias. |

### 4.2 Sincronización con Supabase Cloud (`push` vs `pull`)

Para conectar el repositorio con la nube se requiere vincular previamente el proyecto con:  
`supabase link --project-ref <id_proyecto_cloud>`

| Comando | Dirección | Propósito Operativo |
| :--- | :---: | :--- |
| **`supabase db push`** | **Local -> Nube** | Lee las migraciones locales pendientes en `supabase/migrations/*.sql` y las ejecuta en la base de datos remota de Supabase Cloud para desplegar los cambios probados. |
| **`supabase db pull`** | **Nube -> Local** | Compara el esquema remoto de Supabase Cloud contra el entorno local y descarga cualquier cambio realizado manualmente desde el panel web de la nube en un nuevo archivo de migración local. |

---

## 5. Fundamentación Teórica

Este enfoque responde a los lineamientos expuestos en la literatura empresarial de referencia (*Enterprise Application Patterns using .NET MAUI*, Capítulo 9: *Containerized Microservices*):

1. **Aislamiento de Dependencias:** El desacoplamiento entre el cliente móvil y la infraestructura de datos evita bloqueos por falta de conectividad o mantenimiento en servidores externos.
2. **Reproducibilidad:** La definición declarativa de la base de datos garantiza que cualquier miembro del equipo de desarrollo pueda clonar el repositorio y levantar el entorno exacto en un solo paso.
