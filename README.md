<div align="center">
  
# EcoRuta-App

</div>

<div align="center">


**Aplicación móvil de articulación para la gestión y el aprovechamiento de residuos, dirigida a la ciudadanía, los recicladores de oficio y los prestadores del servicio de aseo y recolección del municipio de Tuluá, con el propósito de fortalecer la comunicación, la coordinación y la economía circular local.**

![Estado](https://img.shields.io/badge/estado-en%20desarrollo-yellow?style=for-the-badge)
![Plataforma](https://img.shields.io/badge/plataforma-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)

</div>


## Herramientas y tecnologías

<div align="center">

<a href="https://developer.android.com/studio"><img src="https://github.com/devicons/devicon/blob/master/icons/androidstudio/androidstudio-original.svg" title="Android Studio" alt="Android Studio" width="40" height="40"/></a>
<a href="https://kotlinlang.org/"><img src="https://github.com/devicons/devicon/blob/master/icons/kotlin/kotlin-original.svg" title="Kotlin" alt="Kotlin" weidt="40" height="40"/></a>
<a href="https://www.figma.com/"><img src="https://github.com/devicons/devicon/blob/master/icons/figma/figma-original.svg" title="Figma" alt="Figma" weidt="40" height="40"/></a>
<a href="https://supabase.com/"><img src="https://github.com/devicons/devicon/blob/master/icons/supabase/supabase-original.svg" title="Supabase" alt="Supabase" weidt="40" height="40"/></a>
<a href="https://trello.com/"><img src="https://github.com/devicons/devicon/blob/master/icons/trello/trello-original.svg" title="Trello" alt="Trello" weidt="40" height="40"/></a>

</div>
<div align="center">
  
  | Herramienta | Uso en el proyecto |
  |---|---|
  | **Android Studio** | IDE para el desarrollo de la app |
  | **Kotlin** | Lenguaje de programación principal |
  | **Figma** | Diseño de interfaces y prototipos |
  | **Supabase** | Base de datos, autenticación y backend |
  | **Trello** | Gestión de tareas y seguimiento del equipo |

</div>


## Estructura del proyecto

El proyecto implementa una arquitectura **Feature-Driven Clean Architecture + MVVM**, separando el código por módulos de negocio y dividiendo cada uno en tres capas: `data` (fuentes de datos y repositorios), `domain` (reglas de negocio puras) y `presentation` (vistas en Compose y ViewModels con StateFlow).

### Arquitectura de la aplicación (`src/main`)

```text
app/src/main/java/com/ecoruta/app/
│
├── EcoRutaApp.kt                        # Inicializador principal de la aplicación
├── MainActivity.kt                      # Ventana contenedora y control de navegación
│
├── core/                                # Componentes y utilidades compartidas
│   ├── common/
│   │   ├── result/                      # Estados de respuesta (éxito, error y carga)
│   │   ├── extensions/                  # Funciones de ayuda y extensiones de UI
│   │   └── utils/                       # Cálculos de GPS, fechas y precios
│   ├── database/                        # Base de datos local Room para modo sin red
│   │   ├── EcoRutaDatabase.kt           # Archivo maestro de la base de datos local
│   │   ├── converter/                   # Conversores para datos complejos
│   │   └── dao/                         # Consultas para lectura y guardado local
│   ├── network/                         # Conectividad externa
│   │   ├── supabase/                    # Conector con la nube de EcoRuta
│   │   └── connectivity/                # Monitor del estado de conexión a internet
│   ├── navigation/                      # Sistema de rutas y navegación
│   │   ├── EcoRutaNavHost.kt            # Grafo principal de pantallas
│   │   ├── NavigationDestination.kt     # Catálogo de rutas de la app
│   │   ├── AuthGuard.kt                 # Guardián de rutas protegidas vs modo visitante
│   │   └── components/                  # Barra inferior dinámica según el rol activo
│   └── ui/                              # Elementos visuales y tema oficial
│       ├── components/                  # Componentes y diálogos transversales
│       │   ├── AuthRequiredDialog.kt    # Diálogo de candado para requerir inicio de sesión
│       │   └── AuthPromptBottomSheet.kt # Hoja inferior con botones de login y registro
│       └── theme/                       # Identidad visual y tema oficial de Figma
│           ├── Color.kt                 # Paleta de colores de la marca: design system
│           ├── Type.kt                  # Estilos y tamaños de texto
│           ├── Shape.kt                 # Radios y formas de esquinas
│           └── Theme.kt                 # Tema base con color dinámico desactivado
│
├── di/                                  # Inyección de dependencias con Dagger Hilt
│   ├── DatabaseModule.kt                # Provisión de base de datos local y DAOs
│   ├── NetworkModule.kt                 # Provisión de cliente Supabase y red
│   └── RepositoryModule.kt              # Enlace de repositorios entre capas
│
└── features/                            # Módulos funcionales de la app móvil
    │
    ├── auth/                            # Ingreso, registro y claves temporales
    │   ├── data/                        # Envío y recepción de credenciales con la nube
    │   ├── domain/                      # Reglas de negocio para cuentas y sesiones
    │   └── presentation/                # Pantallas de login, registro y contraseñas
    │
    ├── profile/                         # Perfil de usuario y cambio entre roles
    │   ├── data/                        # Consulta y actualización de datos de usuario
    │   ├── domain/                      # Reglas de permisos para cada rol
    │   └── presentation/                # Pantalla de perfil, foto y cambio de rol
    │
    ├── reports/                         # Reportes de basura y contenedores llenos
    │   ├── data/                        # Subida de fotos y guardado de coordenadas
    │   ├── domain/                      # Reglas de estados del reporte ciudadano
    │   └── presentation/                # Formulario en 3 pasos e historial con insignias
    │
    ├── routes/                          # Rutas de aseo por barrio y alertas
    │   ├── data/                        # Descarga y guardado de horarios de recolección
    │   ├── domain/                      # Lógica de micro-rutas y distancias
    │   └── presentation/                # Mapa interactivo y control del radar de aviso
    │
    ├── circular_economy/                # Bolsa de reciclaje y entregas con QR
    │   ├── data/                        # Validación de entregas con geocerca y código
    │   ├── domain/                      # Regla de seguridad: distancia < 100m y QR de 5 min
    │   └── presentation/                # Publicar material y escáner de reciclador
    │
    ├── gamification/                    # Beneficios comerciales y EcoPuntos
    │   ├── data/                        # Descuento de puntos y verificación en caja
    │   ├── domain/                      # Control de saldo positivo y stock disponible
    │   └── presentation/                # Catálogo de cupones y validador de comercio
    │
    ├── bulky_waste/                     # Retiro especial de escombros y muebles
    │   ├── data/                        # Registro de citas de recolección de escombros
    │   ├── domain/                      # Ciclo de estados de la solicitud de retiro
    │   └── presentation/                # Calendario semanal y línea de tiempo
    │
    ├── driver/                          # Panel móvil para el chofer de aseo
    │   ├── data/                        # Envío de ubicación GPS en segundo plano
    │   ├── domain/                      # Orden de paradas asignadas para el turno
    │   └── presentation/                # Lista de órdenes y ruta en mapa con paradas
    │
    └── education/                       # Guías ambientales y recompensas
        ├── data/                        # Registro de lecciones pedagógicas completadas
        ├── domain/                      # Regla de +20 EcoPuntos por lección vista
        └── presentation/                # Fichas didácticas de separación de residuos
```

### Estructura de pruebas unitarias (`src/test`)

Las pruebas se ejecutan localmente sobre la JVM y siguen el principio de **estructura en espejo** (*package mirroring*) para mantener trazabilidad directa con cada módulo de la aplicación.

```text
app/src/test/java/com/ecoruta/app/
│
├── ExampleUnitTest.kt                   # Test de verificación base del entorno
│
├── core/                                # Pruebas y utilidades para el núcleo
│   ├── testing/
│   │   ├── MainDispatcherRule.kt        # Regla de JUnit para corrutinas en ViewModels
│   │   └── TestDataFactory.kt           # Generador de datos y modelos simulados (Fakes)
│   └── utils/                           # Pruebas de funciones auxiliares (fechas, GPS)
│
└── features/                            # Pruebas unitarias organizadas por módulo
    ├── auth/                            # Pruebas de login, registro y recuperación
    ├── profile/                         # Pruebas de conmutación de roles y perfil
    ├── reports/                         # Pruebas de validación y estados de reportes
    ├── routes/                          # Pruebas de cobertura de radar y horarios
    ├── circular_economy/                # Pruebas de expiración de QR y geocerca
    ├── gamification/                    # Pruebas de cálculo de saldos y cupones
    ├── bulky_waste/                     # Pruebas del ciclo de retiro de escombros
    ├── driver/                          # Pruebas de asignación y orden de paradas
    └── education/                       # Pruebas de entrega de puntos educativos
```


## Equipo de desarrollo

<div align="center">

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/SantiagoLopezUV">
        <img src="https://github.com/SantiagoLopezUV.png?size=120" width="120" height="120" style="border-radius:50%" alt="Integrante 1"/><br/>
        <sub><b>Santiago López</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/ospina27">
        <img src="https://github.com/ospina27.png?size=120" width="120" height="120" style="border-radius:50%" alt="Integrante 2"/><br/>
        <sub><b>Alejandro Ospina</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/kevinposso">
        <img src="https://github.com/kevinposso.png?size=120" width="120" height="120" style="border-radius:50%" alt="Integrante 3"/><br/>
        <sub><b>Kevin Steven Posso</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/Sebas-Arias2001">
        <img src="https://github.com/Sebas-Arias2001.png?size=120" width="120" height="120" style="border-radius:50%" alt="Integrante 4"/><br/>
        <sub><b>Sebastián Arias</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/JUANPIS12">
        <img src="https://github.com/JUANPIS12.png?size=120" width="120" height="120" style="border-radius:50%" alt="Integrante 5"/><br/>
        <sub><b>Juan Pablo Osorio</b></sub>
      </a>
    </td>
  </tr>
</table>

</div>

---

<div align="center">
Hecho por el equipo de <b>EcoRuta</b>
</div>
