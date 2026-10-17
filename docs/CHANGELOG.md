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

---

## [Fase 3: Verificación de Estudiante con OCR] - 2026-10-01
**Rama:** `feature/setup-dependencies-and-navigation`

### Resumen de la Entrega
Integración del flujo de reconocimiento óptico de caracteres (OCR) para carnets universitarios utilizando Google ML Kit Text Recognition. Implementación del caso de uso `ParseStudentCardOcrUseCase` para identificar palabras clave obligatorias ("SUNEDU", "UNIVERSIDAD", año de vigencia 2024-2030), con soporte de botón explícito para omitir y continuar en estado `PENDING_VERIFICATION`.

### Detalle de Modificaciones por Agente

#### Agente 1: Orquestador & Arquitectura
- `domain/model/StudentCardOcrResult.kt`: Estructura de datos para almacenar el resultado del procesamiento OCR.
- `domain/usecase/ParseStudentCardOcrUseCase.kt`: Lógica de negocio pura para detección de SUNEDU, nombres de universidades peruanas y expresión regular de año de vigencia.

#### Agente 2: UI/UX & Compose Specialist
- `presentation/verification/StudentVerificationScreen.kt`: Interfaz intuitiva con visor guía para captura fotográfica con la cámara (`rememberLauncherForActivityResult`), botón para simular lectura en emuladores y botón explícito "Omitir por ahora" que navega a Home.
- `core/navigation/AppNavGraph.kt`: Enlace con `StudentVerificationScreen` en la ruta `Screen.Verification`.

#### Agente 3: Auth & OCR Verification
- `presentation/verification/StudentVerificationViewModel.kt`: Inicialización de `TextRecognition.getClient()`, conversión de `Bitmap` a `InputImage`, llamada asíncrona a ML Kit y actualización del estado en `AuthRepository`.

#### Agente 5: QA & Pruebas Unitarias
- `app/src/test/java/com/example/estudihambres/domain/usecase/ParseStudentCardOcrUseCaseTest.kt`: Pruebas unitarias para carnet universitario completo (SUNEDU + UNIVERSIDAD + Año), carnet simple, recibos de supermercado (no válidos) y texto vacío.
- `./gradlew testDebugUnitTest`: Aprobación al 100%.

#### Agente 6: GitFlow & Release Manager
- Compilación verificada con `./gradlew assembleDebug`.
- APK `app/build/outputs/apk/debug/app-debug.apk` actualizado.

---

## [Fase 4: Pantalla Principal (Home Dashboard)] - 2026-10-01
**Rama:** `feature/setup-dependencies-and-navigation`

### Resumen de la Entrega
Construcción del Dashboard principal para estudiantes universitarios con diseño Material 3. Incluye barra superior personalizada con saludo y estado del carnet, tarjeta de ahorro acumulado en Soles, barra de búsqueda reactiva por palabras clave y carrusel de filtrado por categorías temáticas (Comida, Herramientas digitales, Transporte). Se cargó el catálogo con promociones requeridas (Bembos 2x1, Spotify/YouTube Premium, Notion Education Pack, etc.).

### Detalle de Modificaciones por Agente

#### Agente 1: Orquestador & Arquitectura
- `domain/model/Promotion.kt`: Entidad de promoción universitaria con tags de descuento, ahorro estimado y marcas asociadas.
- `domain/repository/PromotionRepository.kt`: Contrato para la lectura de beneficios.
- `data/repository/MockPromotionRepositoryImpl.kt`: Proveedor mock de beneficios y descuentos reales para universitarios.

#### Agente 2: UI/UX & Compose Specialist
- `presentation/home/HomeScreen.kt`: Scaffold completo con `TopAppBarDashboard`, badge de estado dinámico (`VERIFIED`, `PENDING_VERIFICATION`), tarjeta destacada de ahorro acumulado, campo de búsqueda con botón de limpieza, carrusel con chips seleccionables y lista de tarjetas estilizadas con esquinas redondeadas de 16.dp.
- `presentation/home/HomeViewModel.kt`: Filtrado reactivo en tiempo real por búsqueda y categoría combinadas.

#### Agente 5: QA & Pruebas Unitarias
- `app/src/test/java/com/example/estudihambres/domain/usecase/PromotionFilteringTest.kt`: Pruebas de integración para verificar presencia de Bembos 2x1, Spotify, Notion y el filtrado por categoría "Comida".
- `./gradlew testDebugUnitTest`: Aprobado al 100%.

#### Agente 6: GitFlow & Release Manager
- Validación de compilación exitosa con `./gradlew assembleDebug`.
- APK de depuración generado.

---

## [Fase 5: Mapa Radar 30 km (Google Maps Compose)] - 2026-10-01
**Rama:** `feature/setup-dependencies-and-navigation`

### Resumen de la Entrega
Implementación de la experiencia de radar interactivo basada en Google Maps Compose. Integra solicitud en tiempo de ejecución de permisos GPS (`ACCESS_FINE_LOCATION`) con fallback a coordenadas del campus universitario (Lima Centro: -12.0833, -77.0428). Muestra más de 8 pines de comercios con ofertas activas filtradas a un radio de hasta 30 km con la fórmula Haversine, y despliega un `ModalBottomSheet` con la ficha de descuento y botón de canje al presionar cualquier marcador.

### Detalle de Modificaciones por Agente

#### Agente 1: Orquestador & Arquitectura
- `domain/model/Place.kt`: Campos agregados para descripción de descuento, badge y distancia en km.
- `domain/usecase/FilterPlacesByDistanceUseCase.kt`: Algoritmo Haversine para filtrar y ordenar lugares dentro de 30 km.
- `data/model/PlaceLocal.kt` y `data/repository/MockPlaceRepository.kt`: Catálogo de 9 locales cercanos dentro del radio y 1 local foráneo a 65 km para validación de filtrado.

#### Agente 2: UI/UX & Compose Specialist & Agente 4: Geolocalización
- `presentation/map/MapScreen.kt`: `GoogleMap` reactivo con `rememberCameraPositionState`, marcador azur para la ubicación del estudiante, marcadores rojos para cada local, banner de estado del radar, botón flotante para recentrar vista y `ModalBottomSheet` para detalles del descuento.
- `presentation/map/MapViewModel.kt`: Manejo de coordenadas actuales vs coordenadas por defecto, cálculo de radio y selección de establecimiento.

#### Agente 5: QA & Pruebas Unitarias
- `app/src/test/java/com/example/estudihambres/domain/usecase/FilterPlacesByDistanceUseCaseTest.kt`: Prueba de exclusión de locales a más de 30 km, retención de al menos 8 locales dentro del radio y ordenamiento por cercanía.
- `./gradlew testDebugUnitTest`: Aprobación al 100%.

#### Agente 6: GitFlow & Release Manager
- Compilación verificada con `./gradlew assembleDebug`.
- APK de depuración generado.

---

## [Fase 6: QA, Verificación Final y Build de APK] - 2026-10-01
**Rama:** `develop` / `feature/setup-dependencies-and-navigation`

### Resumen de la Entrega
Ejecución del ciclo completo de aseguramiento de calidad (QA). Se ejecutaron satisfactoriamente todas las pruebas unitarias automatizadas (`testDebugUnitTest`), se generó y validó el binario APK final en `app/build/outputs/apk/debug/app-debug.apk` y se documentó el informe de pruebas en `docs/QA_REPORT.md`.

### Detalle de Modificaciones por Agente

#### Agente 5: QA & Pruebas Unitarias
- Validación y ejecución de la suite completa de pruebas unitarias:
  - `ValidateStudentCredentialsUseCaseTest`: Formato DNI (8 dígitos), correo universitario y contraseña.
  - `ParseStudentCardOcrUseCaseTest`: Palabras clave SUNEDU, UNIVERSIDAD y vigencia.
  - `FilterPlacesByDistanceUseCaseTest`: Exclusión de comercios > 30 km y retención de locales cercanos.
  - `PromotionFilteringTest`: Presencia de marcas universitarias y filtrado por categoría.
- Creación de `docs/QA_REPORT.md` documentando la totalidad de casos de prueba y métricas de calidad.

#### Agente 6: GitFlow & Release Manager
- Generación y verificación del APK debug en `app/build/outputs/apk/debug/app-debug.apk` (20.9 MB).
- Integración de los cambios hacia la rama `develop` para sincronización con el repositorio remoto.

---

## [Sprint de Estabilidad: Corrección de Bugs Críticos en Dispositivos Físicos] - 2026-10-01
**Rama:** `develop`

### Resumen de la Entrega
Resolución prioritaria de los 4 problemas reportados durante pruebas en dispositivos físicos (Xiaomi / MIUI y emuladores):
1. **Autocomplete de Universidades y eliminación de restricciones de espacios (`RegisterScreen.kt`)**: Reemplazo del campo de texto plano por un `ExposedDropdownMenuBox` interactivo con catálogo de universidades peruanas sugeridas (Universidad Continental, PUCP, UNMSM, UNI, UPC, UTEC, USMP, UCSM, UNSA, etc.), permitiendo escribir espacios sin recorte prematuro (`.trim()`).
2. **Persistencia de sesión de usuario (`SessionManager` & `AppNavGraph.kt`)**: Almacenamiento local mediante `SharedPreferences` de la sesión del estudiante (nombre, correo, código, universidad, estado de verificación y flag de sesión). Al abrir la app, si la sesión existe, se navega directamente al `HomeScreen` sin solicitar credenciales.
3. **Corrección de Crash al pulsar "Tomar Foto" (`StudentVerificationScreen.kt`)**: Solicitud preventiva de permisos de tiempo de ejecución para cámara (`Manifest.permission.CAMERA`) mediante `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`, protección con bloque `try-catch` robusto para evitar caídas en MIUI/Android, y adición de la alternativa "Subir imagen desde galería" mediante `ActivityResultContracts.GetContent()`.
4. **Diagnóstico y Fallback del Radar de Mapa (`MapScreen.kt`)**: Declaración formal de la etiqueta `meta-data` para `com.google.android.geo.API_KEY` en `AndroidManifest.xml` con clave demo e instrucciones de reemplazo; y creación de un botón flotante de alternancia **"Modo Lista / Modo Mapa"** (`ExtendedFloatingActionButton`) que renderiza una lista interactiva de los 9 locales cercanos con sus distancias en km, calificaciones y botón de canje en caso de que los azulejos de Google Maps no carguen por falta de clave activa de Google Cloud.

### Detalle de Modificaciones por Agente

#### Agente 1: Orquestador & Arquitectura
- `core/util/SessionManager.kt`: Creación de la clase singleton con persistencia `SharedPreferences` para los datos del estudiante y banderas de sesión.
- `domain/repository/AuthRepository.kt` & `data/repository/MockAuthRepositoryImpl.kt`: Métodos `saveSession()` y `clearSession()` integrados y sincronizados bidireccionalmente con `SessionManager`.
- `core/navigation/AppNavGraph.kt`: Comprobación inicial de `sessionManager.isUserLoggedIn()` para conmutar `startDestination` dinámicamente entre `Screen.Login.route` y `Screen.Home.route`.

#### Agente 2: UI/UX & Compose Specialist
- `presentation/auth/RegisterScreen.kt`: Implementación de `ExposedDropdownMenuBox` y `ExposedDropdownMenu` con filtrado dinámico en memoria de universidades peruanas; supresión del `.trim()` durante la digitación.
- `presentation/auth/AuthViewModel.kt`: Saneamiento de cadenas trasladado al evento de validación y persistencia de sesión inmediata al iniciar sesión, registrarse o usar bypass.
- `presentation/verification/StudentVerificationScreen.kt`: Botones de acción "Tomar Foto", "Galería" y "Simular Carnet"; decodificación de `Bitmap` segura desde ContentResolver.
- `presentation/map/MapScreen.kt`: Botón flotante extendido para conmutar entre `GoogleMap` y `PlaceListItemCard` en `LazyColumn`, manteniendo el `ModalBottomSheet` activo en ambos modos.

#### Agente 5: QA & Pruebas Unitarias
- `app/src/test/java/com/example/estudihambres/data/MockAuthRepositoryTest.kt`: Pruebas automatizadas de persistencia y actualización de estados (`saveSession`, `updateVerificationStatus`, `clearSession`).
- Ejecución limpia de `./gradlew testDebugUnitTest`: 100% pruebas aprobadas sin fallos.

#### Agente 6: GitFlow & Release Manager
- Compilación final y generación de APK mediante `./gradlew assembleDebug`.
- Sincronización de commits y cambios en la rama `develop`.

---

## [Hotfix: Resolución Integral de Visor de Mapa y Detección OCR en Carnet] - 2026-10-01
**Rama:** `develop`

### Resumen de la Entrega
Solución definitiva a los dos bloqueos visuales y de sensor reportados por el usuario:
1. **Radar de Mapa 100% visible sin necesidad de Google Cloud API Key (`MapScreen.kt`)**:
   - Integración de `InteractiveOsmRadarMap` con OpenStreetMap y Leaflet.js cargado de forma autónoma.
   - Proporciona mapa callejero completo con azulejos de avenidas, calles, comercios y GPS sin pantallas beige o en blanco.
   - Selector en tiempo real (`FilterChip`) y botón flotante para conmutar entre: **Radar Mapa (OSM)**, **Google Maps** y **Lista 30 km**.
   - Presionar cualquier marcador del mapa interactivo abre el `ModalBottomSheet` con el descuento universitario.
2. **Detección OCR de Carnet en Alta Resolución (`StudentVerificationScreen.kt` & `ParseStudentCardOcrUseCase.kt`)**:
   - Reemplazo de `TakePicturePreview` (que capturaba únicamente thumbnails comprimidos ilegibles de 128x128 píxeles) por captura en resolución completa mediante `FileProvider` y `ActivityResultContracts.TakePicture()`.
   - Incorporación de la biblioteca `com.google.mlkit:text-recognition:16.0.1` con modelo embebido offline (`libmlkit_google_ocr_pipeline.so`), eliminando dependencias de descargas de Google Play Services.
   - Previsualización fotográfica del carnet capturado dentro del visor Compose.
   - Tolerancia a acentos ortográficos (Á, É, Í, Ó, Ú) y soporte ampliado para universidades peruanas (Continental, San Marcos, Católica, etc.).
   - Panel de resultados que exhibe el texto leído por ML Kit y botón de aprobación manual ("Aprobar Carnet con Texto Detectado") para garantizar acceso sin trabas.




---

## [Soporte Integral Google Maps Nativo y Localización GPS Activa Fuera de Lima] - 2026-10-01
**Rama:** `develop`

### Resumen de la Entrega
1. **Google Maps Compose Nativo como Vista Predeterminada (`MapScreen.kt`)**:
   - Restablecimiento del visor nativo `GoogleMap` como pantalla inicial predeterminada por solicitud del usuario.
   - Guía completa paso a paso para la activación de **Maps SDK for Android** en Google Cloud Console.
   - Suministro de huella digital SHA-1 de debug (`46:5A:82:5B:F0:27:B8:2E:94:3A:7E:FD:38:6E:D9:69:AC:42:0E:39`) y paquete `com.example.estudihambres`.
2. **Geolocalización en Tiempo Real con GPS Activo (`PlayServicesLocationRepositoryImpl.kt`)**:
   - Inyección formal de `PlayServicesLocationRepositoryImpl` en `MapViewModel` a través de `MapViewModelFactory`.
   - Solicitud de posición activa de alta precisión con `fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY)` si `lastLocation` es nula.
   - Actualización inmediata de coordenadas y desplazamiento dinámico de cámara hacia la ubicación física real del estudiante.
3. **Radar de 30 km Dinámico para Estudiantes Fuera de Lima (`MapViewModel.kt`)**:
   - Si el estudiante se ubica fuera de Lima (Huancayo, Arequipa, Cusco, Trujillo, etc.), los convenios aliados se despliegan automáticamente en radios cercanos (250m a 2.5km) en las calles de su ciudad.
   - Retención estricta del local foráneo a >65 km para cumplir el filtro de radio de 30 km.


---

## [Cámara Embebida CameraX con Detección OCR Automática e Inteligente de Carnet] - 2026-10-01
**Rama:** `develop`

### Resumen de la Entrega
1. **Cámara Embebida en Recuadro con CameraX (`StudentVerificationScreen.kt`)**:
   - Integración nativa de `CameraX` (`PreviewView`, `ImageAnalysis`, `ImageCapture`, `camera-lifecycle`) directamente dentro del recuadro guiado de la pantalla sin abrir aplicaciones externas.
   - Superposición visual (`CardScannerOverlay`) con marco delimitador para carnet universitario (formato ID-1) y botón de captura manual integrado.
2. **Auto-Escaneo Rápido e Inteligente con Google ML Kit**:
   - Procesamiento en streaming mediante `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` analizando los fotogramas en segundo plano cada 300 ms.
   - Tan pronto como el estudiante encuadra su carnet dentro del recuadro, el OCR detecta automáticamente el documento, congela el visor y aprueba la verificación al instante.
3. **Parser Inteligente de Carnet Universitario Peruano (`ParseStudentCardOcrUseCase.kt`)**:
   - Extracción estructurada de campos oficiales: Nombres (`Manuel Fabrizio`), Apellidos (`Callañaupa Cjuiro`), DNI / Código (`72945602`), Universidad (`Universidad Continental`), Carrera (`Ing. de Sistemas e Informática`) y Vigencia (`2026-2027`).
   - Sincronización automática de los datos verificados con `SessionManager` para personalizar el saludo y perfil en `HomeScreen`.
4. **Pruebas Unitarias y Aseguramiento de Calidad**:
   - Adición de caso de prueba unitario exhaustivo en `ParseStudentCardOcrUseCaseTest` con la estructura real del carnet universitario peruano.

---

## [Sprint de Experiencia Visual y Datos Concretos - v1.2.0] - 2026-10-01
**Rama:** `develop`

### Resumen de la Entrega
Incremento de versión a `1.2.0` (`versionCode = 3`) con mejoras sustanciales en la interfaz del dashboard, carruseles horizontales, apertura de enlaces oficiales vía `Intent` de Android, geolocalización de coordenadas comerciales reales y botón de cierre de sesión:

1. **Botón de Cerrar Sesión (`HomeScreen.kt` & `AppNavGraph.kt`)**:
   - Integración de icono de acción `Icons.Default.ExitToApp` en la `TopAppBar` del dashboard principal.
   - Al presionarlo, invoca `sessionManager.clearSession()` y redirige de inmediato a la pantalla de Login, limpiando la pila de navegación (`popUpTo(0) { inclusive = true }`) para prevenir accesos hacia atrás sin credenciales.

2. **Rediseño del Dashboard con Carruseles Horizontales (`HomeScreen.kt`)**:
   - Reemplazo del feed plano vertical por un layout dinámico en `LazyColumn` compuesto de secciones especializadas:
     - **✨ Para ti (Más Populares)** (`LazyRow`): Convenios destacados de alcance global (Spotify Premium, YouTube Premium, Starbucks 2x1, Bembos), con tags de descuento, nombres de comercios aliados y botón directo "Activar beneficio".
     - **📍 Cerca de tu campus** (`LazyRow`): Comercios locales con indicador de proximidad y tiempo a pie (e.g. "150m · 2 min"), precio regular tachado y precio universitario con descuento destacado, junto con botón "Canjear".
     - **💻 Beneficios Virtuales & Tech** (`LazyColumn` item cards): Tarjetas con licencias y software educativo (GitHub Student Developer Pack, Notion Education Plus, Figma for Education) con distintivo "100% GRATIS" y botón "Reclamar licencia ↗".

3. **Redirección Oficial a Sitios Web con Intent (`HomeScreen.kt` & `MapScreen.kt`)**:
   - Incorporación del atributo `val url: String` en las entidades `Promotion` y `Place`.
   - Lanzamiento de navegadores del sistema con `Intent(Intent.ACTION_VIEW, Uri.parse(url))` bajo protección `try-catch` para evitar fallos si no hay navegador predeterminado.
   - Vinculación a sitios oficiales auténticos:
     - Spotify Student: `https://www.spotify.com/pe-es/student/`
     - Notion for Education: `https://www.notion.so/product/notion-for-education`
     - GitHub Student Pack: `https://education.github.com/pack`
     - Figma for Education: `https://www.figma.com/education/`
     - Bembos Promociones: `https://www.bembos.com.pe/promociones`
   - Inclusión del botón "Ir al beneficio" dentro del `ModalBottomSheet` de detalle de locales en el mapa interactivo.

4. **Locales con Coordenadas Reales en el Mapa (`MockPlaceRepository.kt` & `MapViewModel.kt`)**:
   - Eliminación de algoritmos de generación de coordenadas aleatorias o dispersas que ubicaban pines en pistas de aterrizaje o descampados.
   - Definición de locales comerciales reales en Cusco (Real Plaza Cusco, Av. de la Cultura, UNSAAC, Campus Continental Cusco, Cineplanet, Starbucks, Bembos, Papa John's) y Lima.

5. **Aseguramiento de Calidad y Release**:
   - Incremento oficial de versión en `app/build.gradle.kts`: `versionCode = 3`, `versionName = "1.2.0"`.
   - Pruebas unitarias ejecutadas satisfactoriamente y compilación aprobada: `./gradlew testDebugUnitTest assembleDebug`.
   - APK verificado en `app/build/outputs/apk/debug/app-debug.apk`.

---

## [Módulo de Perfil Universitario, Logo Mapache Mamey y Suite QA de 84 Pruebas] - 2026-10-01
**Rama:** `develop`

### Resumen de la Entrega
1. **Configuración de Perfil de Usuario (`ProfileScreen.kt` & `ProfileViewModel.kt`)**:
   - Acceso directo e intuitivo al perfil universitario al pulsar el avatar del estudiante o el botón "Editar >" en `HomeScreen.kt`, el icono de usuario en la `TopAppBar`, o la 4ta pestaña en la barra de navegación inferior ("Mi Perfil").
   - Cabecera estilizada con la mascota oficial Mapache Mamey, nombre del estudiante, universidad, carrera y estado de verificación ("Verificación Pendiente" / "Verificado Oficial").
   - Fila de métricas del estudiante: "Ahorro Total" (e.g. S/ 142.50), "Canjes" (e.g. 8 cupones) y "Rango" (e.g. Nivel Pro ⭐).
   - Formulario interactivo con campos editables: Nombre Completo, DNI (8 dígitos con validación), Universidad de procedencia (con autocomplete desplegable `UniversityDropdownField`), Carrera Universitaria / Facultad, Código de Matrícula y Correo Institucional.
   - Ajustes de convenios y geolocalización: selector de radio de radar GPS (5 km, 15 km, 30 km) y switch de notificaciones de proximidad a comercios (200m).
   - Botones de acción: "Guardar Cambios" con persistencia en `SessionManager`, "Re-escanear Carnet Universitario" (redirige al scanner CameraX) y "Cerrar Sesión" (limpieza de sesión y redirección segura a login).

2. **Identidad Visual y Logo Oficial (`app_logo.webp`)**:
   - Integración de la ilustración oficial del Mapache Mamey (Bibliotecario Raccoon con lentes y delantal de estudio) como icono de la app en `AndroidManifest.xml` (`android:icon="@drawable/app_logo"` y `android:roundIcon="@drawable/app_logo"`).
   - Logo destacado en `LoginScreen.kt`, `HomeScreen.kt` (barra superior y tarjeta de bienvenida), y `ProfileScreen.kt`.

3. **Suite Completa de Aseguramiento de Calidad (>50 Pruebas QA Automatizadas)**:
   - Implementación de 64 pruebas unitarias adicionales en el paquete `com.example.estudihambres.qa`:
     - `AuthAndCredentialsQaTest.kt` (16 tests): validaciones de DNI de 8 dígitos, correos con dominios institucionales (.edu.pe, .edu, .pe), longitud de contraseñas y detección de entradas malformadas.
     - `StudentCardOcrQaTest.kt` (12 tests): reconocimiento de carnet universitario oficial SUNEDU, tolerancia a mayúsculas/minúsculas, extracción de nombres, códigos, universidades peruanas y rechazo de boletas o texto basura.
     - `GeolocationRadarQaTest.kt` (12 tests): cálculo esférico Haversine, límites de radio de 30 km, simetría de distancias, cobertura de comercios en Cusco y Lima.
     - `PromotionsAndDealsCatalogQaTest.kt` (12 tests): integridad de URLs oficiales (Spotify, Notion, GitHub, Figma), filtrado por categorías y búsqueda sin distinción de mayúsculas.
     - `UserProfileAndSessionQaTest.kt` (12 tests): modelo `StudentUser`, persistencia y transiciones de estados de verificación, cálculo de rangos ("Estudiante Pro"), acumulación de ahorros y sanitización de campos.
   - **Total de pruebas en el proyecto: 84 pruebas unitarias ejecutadas con 100% de éxito.**

4. **Compilación, Verificación en Emulador y Release**:
   - Compilación completa sin advertencias: `./gradlew testDebugUnitTest assembleDebug` (Build Successful).
   - Verificación funcional y estética en emulador Android físico/virtual con capturas de pantalla de Login, Home y Perfil.

---

## [Corrección Crítica de Autenticación, Estabilización OCR, Carnet Único y Navegación Turn-by-Turn - v1.2.1] - 2026-10-17
**Rama:** `develop`

### Resumen de la Entrega
Resolución integral de las fallas críticas reportadas en pruebas en dispositivo físico:
1. **Validación Estricta de Autenticación y Credenciales (`AuthViewModel.kt`, `SessionManager.kt`, `MockAuthRepositoryImpl.kt`)**:
   - Corrección del fallo de acceso que permitía ingresar con contraseñas incorrectas o correos inexistentes.
   - Implementación de doble persistencia en `SessionManager`: almacén de cuentas registradas (`PREF_ACCOUNTS_NAME`) que sobrevive al cierre de sesión (`clearSession()`), y almacén de sesión activa (`PREF_SESSION_NAME`).
   - Verificación estricta de contraseña y correo institucional en `AuthRepository.authenticate()` y `AuthRepository.register()`.
   - Banner visual de error en rojo en `LoginScreen.kt` y `RegisterScreen.kt` notificando "Contraseña incorrecta" o "No existe ninguna cuenta registrada".
   - Cuenta de prueba institucional pre-cargada: `alumno@continental.edu.pe` / Contraseña: `campus123` / DNI: `72945602`.

2. **Diferenciación de Modo Desarrollador (Vista Genérica) vs. Estudiante Real (`StudentUser.kt`, `HomeScreen.kt`, `ProfileScreen.kt`)**:
   - El botón "Bypass Dev" asigna explícitamente `isDev = true` con perfil genérico (`"Modo Desarrollador (Vista Genérica)"`).
   - Presentación de tarjeta destacada `🛠️ MODO DESARROLLADOR (VISTA GENÉRICA)` en `HomeScreen.kt` y `ProfileScreen.kt` que detalla los permisos de QA para pruebas de laboratorio.
   - Los estudiantes registrados visualizan su interfaz personalizada con su nombre completo, universidad, carrera, código y estado de verificación real.

3. **Escáner Inteligente con Estabilización Multi-Frame y Rechazo Estricto (`ParseStudentCardOcrUseCase.kt`, `StudentVerificationScreen.kt`)**:
   - Filtro de rechazo inmediato para boletas, facturas, comprobantes comerciales y farmacias (`BOLETA`, `FACTURA`, `PANADOL`, `RUC`, `FARMACIA`, `TOTAL S/`).
   - Estabilización multi-frame en `EmbeddedCameraCardScanner`: la cámara ya no se activa precipitadamente con un solo frame. Requiere al menos 4 frames consecutivos verificados (~1.5 a 2 segundos continuos) con análisis sostenido.
   - HUD dinámico con barra de progreso (`0% -> 25% -> 50% -> 75% -> 100%`) y mensajes en tiempo real:
     - "Alinea el carnet oficial con sello SUNEDU en el recuadro..."
     - "Sello institucional detectado (25%). Mantén firme..."
     - "Universidad y código identificados (50%). Verificando..."
     - "Validando sello oficial de SUNEDU (75%)..."
     - "¡Carnet verificado correctamente! (100%)"
     - En caso de apuntar a un recibo comercial: "Comprobante no válido. Requiere carnet con sello SUNEDU".

4. **Regla de Negocio: Restricción de Carnet Único para Estudiantes (`SessionManager.kt`, `StudentVerificationViewModel.kt`)**:
   - Un estudiante regular solo puede vincular un único carnet universitario que coincida con su DNI registrado. Se bloquea el escaneo de carnets ajenos o múltiples carnets.
   - Si un estudiante ya verificado intenta escanear otro carnet, se muestra una alerta: "Tu cuenta ya cuenta con un carnet validado oficialmente. Solo tienes permitido registrar 1 carnet."
   - El modo desarrollador (`isDev = true`) mantiene libertad ilimitada para re-escanear y realizar pruebas de integración de múltiples carnets.

5. **Conexión Real con Locales Aliados y Navegación Turn-by-Turn en Google Maps (`MockPlaceRepository.kt`, `MapScreen.kt`)**:
   - Incorporación de botón directo `"🗺️ Cómo llegar (Google Maps)"` tanto en las tarjetas de la lista de 30 km como en el `ModalBottomSheet` de detalle.
   - Disparo de navegación nativa turn-by-turn con `Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))` con fallback a la web oficial de Google Maps si la aplicación no está instalada.
   - Locales comerciales reales con coordenadas exactas en Cusco (Real Plaza Cusco, Av. de la Cultura, UNSAAC, Campus Continental) y Lima (UNMSM, PUCP, Av. Universitaria).

6. **Aseguramiento de Calidad y Suite de 92 Pruebas QA**:
   - Incorporación de 8 nuevas pruebas unitarias y de QA validando rechazo de contraseñas erróneas, cuentas inexistentes, duplicados, modo Dev, carnet único y URLs de navegación.
   - Ejecución de `testDebugUnitTest` y `assembleDebug`: 92 pruebas ejecutadas con 100% de éxito (`BUILD SUCCESSFUL in 27s`).

---

## [Auditoría de Seguridad, Inmutabilidad de Identidad Académica y Avatar de Perfil Modificable - v1.2.2] - 2026-10-17
**Rama:** `develop`

### Resumen de la Entrega
Auditoría integral de seguridad, reforzamiento visual y hardening de campos sensibles según solicitud de auditoría:
1. **Inmutabilidad Estricta de Campos Oficiales de Identidad (`ProfileScreen.kt`, `ProfileViewModel.kt`, `SessionManager.kt`)**:
   - Los campos **DNI** y **UNIVERSIDAD** quedan permanentemente bloqueados para edición por parte del usuario, al constituir acreditaciones oficiales validadas mediante SUNEDU y RENIEC.
   - En `ProfileScreen.kt`, se sustituyó el dropdown interactivo de universidades y el campo de texto editable de DNI por `OutlinedTextField` de solo lectura (`readOnly = true`) acompañados de:
     - Icono de candado de seguridad `Icons.Default.Lock`.
     - Textos de apoyo explicativos: `"🔒 Bloqueado: Validado oficialmente por SUNEDU/RENIEC"` y `"🔒 Bloqueado: Acreditación académica no modificable"`.
   - En `SessionManager.kt` y `ProfileViewModel.kt`, la función `updateProfile()` asegura a nivel de lógica de negocio y persistencia que los valores de `dni` y `university` provienen exclusivamente de la sesión verificada y no pueden ser sobreescritos ni alterados por payloads de cliente.

2. **Personalización y Modificación Dinámica de la Foto de Perfil (`ProfileScreen.kt`, `HomeScreen.kt`, `SessionManager.kt`, `StudentUser.kt`)**:
   - Integración de selector nativo de imágenes con `ActivityResultContracts.GetContent()` para permitir al estudiante cambiar su avatar desde la galería del dispositivo.
   - Copia segura del archivo en almacenamiento interno aislado (`context.filesDir/avatar_*.jpg`), evitando problemas de expiración de URIs y persistiendo la ruta en `KEY_PROFILE_PHOTO_PATH` de `SessionManager`.
   - Visualización reactiva del avatar personalizado en:
     - `ProfileScreen.kt`: En `ProfileHeaderCard` con badge superpuesto de cámara `Icons.Default.PhotoCamera` y botón dedicado `"Cambiar Foto de Perfil"`.
     - `HomeScreen.kt`: En la barra superior `TopAppBarDashboard` y en la tarjeta de saludo institucional `StudentGreetingHeader`.
     - Fallback automático al logotipo oficial (Mapache Mamey) cuando no existe foto personalizada.

3. **Pruebas de Calidad QA y Verificación de Compilación**:
   - Incorporación de pruebas en `UserProfileAndSessionQaTest.kt`:
     - `testDniAndUniversityImmutabilityOnProfileUpdate()`: Comprueba la inmutabilidad de DNI y Universidad ante intentos de modificación de perfil.
     - `testProfilePhotoPersistenceAndPath()`: Comprueba la asignación y persistencia de `profilePhotoPath`.
   - Ejecución exitosa de la suite completa de pruebas unitarias y generación de APK debug: `./gradlew testDebugUnitTest assembleDebug` (`BUILD SUCCESSFUL in 39s`).

4. **Blindaje Preventivo de Intents y Cero Advertencias de Compilación**:
   - Protección contra `ActivityNotFoundException` mediante bloques `try-catch` robustos en la resolución de URLs externas y navegación nativa de Google Maps.
   - Migración completa de iconos `Icons.Default.ExitToApp` y `Icons.Default.OpenInNew` hacia sus versiones oficiales reflejadas `Icons.AutoMirrored.Filled.*`.
   - Depuración de imports no utilizados y actualización de `centerAlignedTopAppBarColors` a `topAppBarColors`, alcanzando compilación limpia con **0 advertencias y 0 errores** (`BUILD SUCCESSFUL in 21s`).

---

## [Intuitividad, Mejoras de UX, Navegación Fluida y Suite QA v1.2.3] - 2026-10-17
**Rama:** `develop` / `main`

### Resumen de la Entrega
Optimización holística de la experiencia de usuario (UX) e intuitividad del sistema para estudiantes universitarios:
1. **Flujo de Acceso y Registro Ágil e Intuitivo (`LoginScreen.kt`, `RegisterScreen.kt`)**:
   - Selector interactivo de visibilidad de contraseña (`IconButton` con `Icons.Default.Visibility` y `Icons.Default.VisibilityOff`) con soporte de teclado IME (`ImeAction.Next` e `ImeAction.Done`).
   - Sugerencias rápidas en un solo tap de las principales universidades peruanas ("Continental", "PUCP", "UNMSM", "UNI", "UTEC", "UNSAAC") en el registro.
   - Contador dinámico de dígitos en vivo para el DNI (`${state.dni.length}/8`) con indicador visual de confirmación al completar los 8 dígitos numéricos.
   - Tip contextual institucional en el login ("💡 Tip: Ingresa con tu correo institucional (.edu.pe)...").

2. **Guía Visual para Validación de Carnet (`StudentVerificationScreen.kt`)**:
   - Acordeón interactivo expandible "💡 Consejos para un escaneo exitoso" con recomendaciones claras (superficie plana, buena iluminación, sin reflejos ni boletas comerciales).
   - HUD multi-frame enriquecido con retroalimentación paso a paso durante el análisis OCR.

3. **Feed Principal y Navegación Mejorada (`HomeScreen.kt`, `AppNavGraph.kt`)**:
   - Banner de llamada a la acción (CTA) de alto contraste para estudiantes no verificados con acceso directo en un tap a la cámara de verificación.
   - Chips de búsqueda rápida debajo del buscador (`Bembos`, `Spotify`, `GitHub`, `Café`, `Notion`, `2x1`).
   - `PromotionDetailSheet` (`ModalBottomSheet`) con información completa de cada beneficio, categoría y guía paso a paso para el canje estudiantil.
   - Enlace directo "Ver en mapa ›" desde la sección de locales cercanos al campus hacia el radar de 30 km.
   - Diálogo de confirmación modal (`AlertDialog`) antes de cerrar sesión para evitar salidas accidentales.

4. **Radar de Beneficios y Filtros por Categoría (`MapScreen.kt`, `MockPlaceRepository.kt`)**:
   - Chips horizontales de filtrado rápido por categoría en el radar: `"Todos"`, `"Comida"`, `"Café"`, `"Librería"`.
   - Incorporación de comercios académicos esenciales ("Librería & Copias Universitaria Cusco" y "Librería & Papelería San Marcos").
   - Filtrado reactivo tanto en OpenStreetMap como en Google Maps y en la vista de lista.

5. **Centro de Ayuda y Preguntas Frecuentes (`ProfileScreen.kt`)**:
   - Acordeón interactivo de FAQs (`FaqAccordionItem`) que explica de forma transparente el por qué de la inmutabilidad de DNI/Universidad, la privacidad del OCR local sin subida a servidores externos y la dinámica de canje en locales físicos.
   - Diálogo de confirmación modal para cierre seguro de sesión.

6. **Suite Integral de 105 Pruebas QA Automatizadas (`IntuitiveUxAndNavigationQaTest.kt`)**:
   - Nueva suite de pruebas cubriendo chips rápidos, filtrado por categorías, validación de DNI, dominios `.edu.pe`, metadatos de promociones e integridad de FAQs.
   - Cobertura de 105 pruebas unitarias exitosas con 100% de aprobación (`BUILD SUCCESSFUL in 13s`).
---

## [Optimización y Rediseño Integral del Radar de Mapas v1.2.4] - 2026-10-17
**Rama:** `develop` / `main`

### Resumen de la Entrega
Mejora integral del sistema de mapas y radar geográfico de convenios universitarios:
1. **Buscador en Vivo en el Mapa (`MapScreen.kt`, `MapViewModel.kt`)**:
   - Integración de campo de búsqueda en tiempo real dentro del banner flotante superior con filtrado reactivo por nombre, dirección, categoría y etiqueta de descuento.
   - Botón de limpieza rápida para restablecer el término buscado en 1 toque.

2. **Selector Dinámico de Radio de Radar (`MapViewModel.kt`, `MapScreen.kt`)**:
   - Chips de selección rápida de radio: `⚡ 5 km` (a pie / campus inmediato), `🚲 15 km` (bici / transporte) y `🚗 30 km` (todo el radar metropolitano).
   - El filtrado por distancia recalcula instantáneamente los locales visibles mediante el algoritmo esférico de Haversine.

3. **Alternancia Rápida de Sedes Universitarias (Cusco / Lima)**:
   - Enum `CampusLocation` con coordenadas oficiales:
     - `Cusco (Campus Continental / UNSAAC)`: `-13.5230, -71.9480`.
     - `Lima (Campus UNMSM / PUCP)`: `-12.0833, -77.0428`.
   - Chips interactivos en la cabecera que permiten al estudiante o evaluador conmutar entre campus y animar automáticamente la cámara hacia la sede seleccionada.

4. **Círculo Visual de Cobertura de Radar**:
   - **Google Maps:** Componente `Circle` nativo con relleno translúcido (`#3344EE` con 8% de opacidad) y borde definido que ilustra el alcance físico del radio configurado.
   - **OpenStreetMap / Leaflet:** Incorporación de `L.circle` dinámico centrado en la posición del estudiante/campus con radio exacto en metros.

5. **Carrusel Inferior de Previsualización de Locales (`PlaceMapPreviewCard`)**:
   - Carrusel flotante horizontal (`LazyRow`) que muestra tarjetas compactas de comercios directamente sobre el mapa.
   - Al pulsar una tarjeta, la cámara de Google Maps vuela suavemente hacia las coordenadas del local y despliega su información detallada.

6. **Pines Cromáticos en Leaflet y Cálculo de Tiempo de Caminata (ETA)**:
   - Pines diferenciados por color en Leaflet: Naranja para Gastronomía, Ámbar para Cafeterías, Cyan para Librerías/Fotocopias, Púrpura para Entretenimiento.
   - Indicador de tiempo estimado de caminata: `🚶‍♂️ ~X min a pie` calculado a ~12 min/km promedio y `🚗 ~Y min en auto`.

7. **Ampliación de Suite QA a 109 Pruebas Unitarias Automatizadas**:
   - 4 nuevas pruebas en `IntuitiveUxAndNavigationQaTest.kt` validando coordenadas de sedes, filtrado estricto a 5 km, búsqueda textual y fórmula de ETA de caminata.
   - 109 pruebas ejecutadas con 100% de éxito y cero advertencias de compilación.
