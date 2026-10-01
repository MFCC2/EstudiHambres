# CHANGELOG - CampusPass (EstudiHambres)

Registro cronológico de modificaciones, agentes intervinientes y cambios arquitectónicos del proyecto de acuerdo con `PROJECT_RULES.md`.

---

## [Fase 1: Setup Base, Dependencias y Clean Architecture] - 2026-09-30
**Rama:** `feature/setup-dependencies-and-navigation`

### Resumen de la Entrega
Configuración inicial del proyecto integrando dependencias oficiales para Jetpack Compose, Material 3, Navigation, Google Maps, Play Services Location y Google ML Kit OCR. Estructuración del árbol de código en 4 capas según Clean Architecture: `core/`, `data/`, `domain/` y `presentation/` con sus submódulos correspondientes.

### Detalle de Modificaciones por Agente

#### Agente 1: Orquestador & Arquitectura (Clean Architecture)
- **Estructura de paquetes implementada**:
  - `core/` (Navegación, Tema y Utilidades):
    - `core/navigation/Screen.kt`: Rutas de navegación (`Home`, `Map`, `Auth`, `Verification`).
    - `core/navigation/AppNavGraph.kt`: Scaffold central con `NavigationBar` y `NavHost`.
    - `core/theme/Color.kt`, `core/theme/Shape.kt`, `core/theme/Type.kt`, `core/theme/Theme.kt`: Identidad corporativa de CampusPass (#3344EE, #00C853, #FF6D00) y esquinas de 16.dp.
    - `core/util/Constants.kt`: Constantes de distancia (radio 30 km, radio terrestre).
    - `core/util/Resource.kt`: Envoltorio genérico de estados asíncronos (`Success`, `Error`, `Loading`).
  - `domain/` (Modelos de Entidad y Contratos):
    - `domain/model/Coupon.kt`: Entidad de cupón universitario.
    - `domain/model/Place.kt`: Entidad de local o restaurante con geolocalización.
    - `domain/model/Commerce.kt`: Entidad de comercio asociado con distancia calculada.
    - `domain/model/StudentUser.kt`: Entidad de estudiante universitario.
    - `domain/model/VerificationStatus.kt`: Enum de verificación (`VERIFIED`, `PENDING_VERIFICATION`, `UNVERIFIED`, `REJECTED`).
    - `domain/repository/CouponRepository.kt`: Contrato para catálogo de cupones.
    - `domain/repository/PlaceRepository.kt`: Contrato para consulta de lugares.
    - `domain/repository/CommerceRepository.kt`: Contrato para convenios de comercios.
    - `domain/repository/AuthRepository.kt`: Contrato para autenticación y carnet.
    - `domain/repository/LocationRepository.kt`: Contrato para geolocalización.
    - `domain/usecase/FilterCommercesByDistanceUseCase.kt`: Algoritmo Haversine para filtrar en radio de 30 km.
  - `data/` (Modelos Locales y Repositorios Mock):
    - `data/model/CouponLocal.kt`: Modelo de cupón local con mapeador `.toDomain()`.
    - `data/model/PlaceLocal.kt`: Modelo de lugar local con mapeador `.toDomain()`.
    - `data/repository/MockCouponRepository.kt`: Datos simulados de cupones para pruebas.
    - `data/repository/MockPlaceRepository.kt`: Datos simulados de establecimientos aliados.
    - `data/repository/MockAuthRepositoryImpl.kt`: Gestión de sesión en memoria con estado inicial `PENDING_VERIFICATION`.
    - `data/repository/MockCommerceRepositoryImpl.kt`: Proveedor mock de comercios con distancias para testing.
    - `data/repository/PlayServicesLocationRepositoryImpl.kt`: Implementación con `FusedLocationProviderClient`.
  - `presentation/` (Submódulos UI):
    - `presentation/auth/AuthScreen.kt`: Pantalla de inicio de sesión con correo institucional.
    - `presentation/verification/OcrScanScreen.kt`: Escaneo de carnet con soporte para omitir a `PENDING_VERIFICATION`.
    - `presentation/home/HomeScreen.kt`: Feed principal de anuncios y cupones.
    - `presentation/map/MapScreen.kt`: Vista previa del mapa interactivo.

#### Agente 2: UI/UX & Compose Specialist
- **Diseño**:
  - Implementación estricta de `RoundedCornerShape(16.dp)` para todas las tarjetas (`Card`).
  - Paleta de colores oficial: Primario Azul/Índigo `#3344EE`, Éxito `#00C853`, Naranja `#FF6D00`.
  - Componentes nativos de Material 3 (`Scaffold`, `NavigationBar`, `NavigationBarItem`, `Card`, `OutlinedTextField`).
  - `MainActivity.kt` adaptado para Edge-to-Edge y montaje de `CampusPassTheme` con `AppNavGraph`.

#### Agente 3: Auth & OCR y Agente 4: Geolocalización
- **Dependencias en `gradle/libs.versions.toml`**:
  - `androidx-navigation-compose = "2.8.8"`
  - `androidx-compose-material-icons-extended`
  - `maps-compose = "6.4.1"`
  - `play-services-maps = "19.0.0"`
  - `play-services-location = "21.3.0"`
  - `play-services-mlkit-text-recognition = "19.0.1"`
- **Permisos en `AndroidManifest.xml`**:
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_FINE_LOCATION`
  - `android.permission.ACCESS_COARSE_LOCATION`
  - `android.permission.CAMERA`
  - Feature no requerido `android.hardware.camera`.

#### Agente 5: QA & Pruebas Unitarias
- `app/src/test/java/com/example/estudihambres/domain/usecase/FilterCommercesByDistanceUseCaseTest.kt`:
  - Prueba de cálculo de distancias esféricas mediante Haversine.
  - Validación de exclusión de locales a más de 30 km.
  - Resultado: `./gradlew testDebugUnitTest` exitoso (100% aprobado).

#### Agente 6: GitFlow & Release Manager
- Rama de trabajo: `feature/setup-dependencies-and-navigation`.
- Validación de compilación: `./gradlew assembleDebug` exitoso.
- Binario verificado: `app/build/outputs/apk/debug/app-debug.apk`.

---

## [Fase 2: Módulo Auth (Login y Registro Estudiantil)] - 2026-10-01
**Rama:** `feature/setup-dependencies-and-navigation`

### Resumen de la Entrega
Implementación completa del flujo de autenticación para estudiantes universitarios con diseño Material 3. Formularios para Login y Registro con captura de DNI (8 dígitos), Código de Alumno, Universidad, Correo institucional y Contraseña. Se implementó `AuthViewModel` desacoplado y el caso de uso `ValidateStudentCredentialsUseCase` para validar DNI, email institucional y contraseña, además de un botón de Bypass para agilizar pruebas locales.

### Detalle de Modificaciones por Agente

#### Agente 1: Orquestador & Arquitectura
- `domain/usecase/ValidateStudentCredentialsUseCase.kt`: Caso de uso puro de validación para DNI de 8 dígitos numéricos, formato de correo universitario (.edu / .edu.pe) y contraseña.
- `presentation/auth/AuthViewModel.kt`: Gestión reactiva con `StateFlow` (`loginState`, `registerState`), interacción con repositorio y función `bypass()`.

#### Agente 2: UI/UX & Compose Specialist
- `presentation/auth/LoginScreen.kt`: Formulario estilizado con Material 3, tarjetas redondeadas a 16.dp, mensajes de error en tiempo real, campos de correo, contraseña y botón explícito de **Bypass Dev**.
- `presentation/auth/RegisterScreen.kt`: Formulario completo de registro con validaciones para DNI, código de alumno, universidad y correo institucional.
- `core/navigation/AppNavGraph.kt`: Integración de `AuthViewModel` inyectado a nivel de grafo, navegación condicional y soporte para transición fluida al flujo de verificación.

#### Agente 5: QA & Pruebas Unitarias
- `app/src/test/java/com/example/estudihambres/domain/usecase/ValidateStudentCredentialsUseCaseTest.kt`: Pruebas de validación de DNI (8 dígitos exactos), formatos de correo válidos e inválidos y longitudes de contraseña.
- `./gradlew testDebugUnitTest`: 100% de pruebas aprobadas.

#### Agente 6: GitFlow & Release Manager
- Compilación verificada con `./gradlew assembleDebug`.
- APK de depuración validado.
