# Arquitectura del Sistema - CampusPass (EstudiHambres)

Documento arquitectónico basado en Clean Architecture y Jetpack Compose según las especificaciones de `PROJECT_RULES.md`.

---

## 1. Diagrama de Capas (Clean Architecture)

```mermaid
flowchart TD
    subgraph PresentationLayer["Presentation Layer (Jetpack Compose + Material 3)"]
        AUTH["auth/ (AuthScreen)"]
        VERIF["verification/ (OcrScanScreen)"]
        HOME["home/ (HomeScreen)"]
        MAP["map/ (MapScreen)"]
    end

    subgraph CoreLayer["Core Layer (Cross-Cutting)"]
        NAV["navigation/ (Screen, AppNavGraph)"]
        THEME["theme/ (CampusPassTheme, Color, Shape 16dp, Type)"]
        UTIL["util/ (Constants, Resource)"]
    end

    subgraph DomainLayer["Domain Layer (Pure Kotlin / Coroutines)"]
        UC["usecase/ (FilterCommercesByDistanceUseCase)"]
        MODELS["model/ (Coupon, Place, Commerce, StudentUser, VerificationStatus)"]
        REPO_INTERFACES["repository/ (CouponRepository, PlaceRepository, AuthRepository, LocationRepository)"]
    end

    subgraph DataLayer["Data Layer (Implementations & Local Sources)"]
        LOCAL_MODELS["model/ (CouponLocal, PlaceLocal)"]
        MOCK_REPOS["repository/ (MockCouponRepository, MockPlaceRepository, MockAuthRepositoryImpl)"]
        PLAY_SERVICES["repository/ (PlayServicesLocationRepositoryImpl)"]
    end

    PresentationLayer --> CoreLayer
    PresentationLayer --> DomainLayer
    MOCK_REPOS --> LOCAL_MODELS
    MOCK_REPOS --> REPO_INTERFACES
    PLAY_SERVICES --> REPO_INTERFACES
    UC --> MODELS
    UC --> REPO_INTERFACES
```

---

## 2. Estructura de Paquetes en `app/src/main/java/com/example/estudihambres/`

```
com.example.estudihambres/
│
├── core/                                 # Navegación, Tema y Utilidades transversales
│   ├── navigation/
│   │   ├── Screen.kt                     # Rutas: Home, Map, Auth, Verification
│   │   └── AppNavGraph.kt                # Scaffold + NavigationBar + NavHost
│   ├── theme/
│   │   ├── Color.kt                      # Paleta: #3344EE, #00C853, #FF6D00
│   │   ├── Shape.kt                      # RoundedCornerShape(16.dp)
│   │   ├── Type.kt                       # Material 3 Typography
│   │   └── Theme.kt                      # CampusPassTheme
│   └── util/
│       ├── Constants.kt                  # Radio máximo 30 km, radio terrestre
│       └── Resource.kt                   # Envoltorio de estados (Success, Error, Loading)
│
├── data/                                 # Modelos locales y repositorios mock
│   ├── model/
│   │   ├── CouponLocal.kt                # Modelo local con mapper .toDomain()
│   │   └── PlaceLocal.kt                 # Modelo local con mapper .toDomain()
│   └── repository/
│       ├── MockCouponRepository.kt       # Repositorio mock de cupones
│       ├── MockPlaceRepository.kt        # Repositorio mock de lugares
│       ├── MockAuthRepositoryImpl.kt     # Repositorio mock de sesión/carnet
│       ├── MockCommerceRepositoryImpl.kt # Comercios mock para pruebas
│       └── PlayServicesLocationRepositoryImpl.kt # FusedLocationProviderClient
│
├── domain/                               # Modelos de entidad y casos de uso
│   ├── model/
│   │   ├── Coupon.kt                     # Entidad de cupón
│   │   ├── Place.kt                      # Entidad de lugar/comercio
│   │   ├── Commerce.kt                   # Entidad comercial ampliada
│   │   ├── StudentUser.kt                # Entidad del estudiante
│   │   └── VerificationStatus.kt         # Enum (VERIFIED, PENDING_VERIFICATION, etc.)
│   ├── repository/
│   │   ├── CouponRepository.kt           # Interfaz de cupones
│   │   ├── PlaceRepository.kt            # Interfaz de lugares
│   │   ├── CommerceRepository.kt         # Interfaz de comercios
│   │   ├── AuthRepository.kt             # Interfaz de autenticación
│   │   └── LocationRepository.kt         # Interfaz de geolocalización
│   └── usecase/
│       └── FilterCommercesByDistanceUseCase.kt # Lógica Haversine (radio <= 30 km)
│
└── presentation/                         # Submódulos de interfaz de usuario
    ├── auth/
    │   └── AuthScreen.kt                 # Ingreso con correo institucional y DNI
    ├── verification/
    │   └── OcrScanScreen.kt              # Escaneo de carnet con opción de omitir
    ├── home/
    │   └── HomeScreen.kt                 # Pantalla principal con anuncios y bienvenida
    └── map/
        └── MapScreen.kt                  # Pantalla de mapa y geolocalización
```

---

## 3. Estado de los Módulos

| Módulo / Agente | Estado | Descripción |
|---|---|---|
| **Agente 1: Arquitectura** | ✅ Configurado | Separación estricta en `core/`, `data/`, `domain/`, `presentation/`. |
| **Agente 2: UI/UX & Compose** | ✅ Configurado | Material 3, `NavigationBar`, `Scaffold`, paleta corporativa y tarjetas redondeadas a 16.dp. |
| **Agente 3: Auth & OCR** | ⏳ Base lista | Dependencia `play-services-mlkit-text-recognition` configurada; `AuthScreen` y `OcrScanScreen` preparadas. |
| **Agente 4: Geolocalización** | ⏳ Base lista | Dependencias `maps-compose` y `play-services-location` agregadas; permisos en manifest y `MapScreen` listo. |
| **Agente 5: QA & Pruebas** | ✅ Activo | Pruebas unitarias de distancia Haversine y filtro de 30 km aprobadas. |
| **Agente 6: GitFlow & Release Manager** | ✅ Verificado | Rama `feature/setup-dependencies-and-navigation` compila sin errores y genera `app-debug.apk`. |
