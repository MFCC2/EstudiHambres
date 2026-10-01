# AGENTES Y REGLAS DE DESARROLLO - CAMPUSPASS

## Regla General de Documentación (Agente Documentador)

Todo cambio, nueva pantalla, función o dependencia agregada DEBE documentarse automáticamente en:

1. `docs/CHANGELOG.md`: Registro de qué agente actuó, qué archivo cambió y por qué.
2. `docs/ARCHITECTURE.md`: Diagrama de componentes y estado de los módulos.
3. KDoc en cada clase y ViewModel creado.

---

## Definición de Agentes

### Agente 1: Orquestador & Arquitectura (Clean Architecture)

- Mantiene la separación: `data/`, `domain/`, `presentation/`.
- Usa Kotlin Coroutines + StateFlow + Jetpack Compose con Material 3.
- No permite lógica de negocio dentro de los composables.

### Agente 2: UI/UX & Compose Specialist

- Replica fielmente el diseño de CampusPass:
  - Tarjetas redondeadas (`RoundedCornerShape(16.dp)`).
  - Tipografías limpias y componentes Material 3 (`Scaffold`, `NavigationBar`, `Card`).
  - Paleta de colores: Primario Azul/Índigo (#3344EE), Verde éxito (#00C853), Naranja (#FF6D00).

### Agente 3: Auth & OCR Verification

- Maneja el estado de sesión y validación de estudiante.
- Implementa `Google ML Kit Text Recognition` para extraer patrones del carnet.
- Si el usuario decide omitir el paso, marca el estado como `PENDING_VERIFICATION` sin bloquear el acceso a la app.

### Agente 4: Geolocalización & Google Maps

- Gestiona permisos en tiempo de ejecución (`ACCESS_FINE_LOCATION`).
- Utiliza `com.google.maps.android:maps-compose`.
- Filtra comercios en un radio euclidiano/haversine de hasta 30 km alrededor de la ubicación del usuario.

### Agente 5: QA & Pruebas Unitarias

- Escribe pruebas unitarias con JUnit 4/5 y MockK en `test/`.
- Valida:
  - Formato de DNI y correo universitario.
  - Parser de texto del OCR del carnet.
  - Filtro de distancia de 30 km para comercios.

### Agente 6: GitFlow & Release Manager

- Valida que el código compile (`./gradlew assembleDebug`) antes de sugerir un commit.
- Usa commits semánticos (`feat`, `fix`, `refactor`, `docs`, `test`).
- Genera el APK en `app/build/outputs/apk/debug/app-debug.apk`.
