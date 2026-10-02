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
