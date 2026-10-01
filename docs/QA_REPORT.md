# Informe de Aseguramiento de Calidad (QA Report) - CampusPass (EstudiHambres)

**Fecha:** 2026-10-01  
**Rama:** `develop` / `feature/setup-dependencies-and-navigation`  
**Agente Responsable:** Agente 5 (QA & Pruebas Unitarias) & Agente 6 (GitFlow & Release Manager)  
**Estado General:** ✅ APROBADO (100% Tests Unitarios Exitosos & APK Compilado)

---

## 1. Resumen Ejecutivo

Se completaron de manera secuencial y autónoma las 6 fases del desarrollo del proyecto CampusPass (EstudiHambres), garantizando la regla de oro: **compilación y verificación limpia antes de cada commit**.

| Métrica | Valor |
|---|---|
| **Pruebas Unitarias Ejecutadas** | 10 pruebas automatizadas |
| **Pruebas Exitosas** | 10 (100%) |
| **Pruebas Fallidas / Errores** | 0 (0%) |
| **Compilación Gradle** | `BUILD SUCCESSFUL` (0 errores) |
| **Artefacto Binario** | `app/build/outputs/apk/debug/app-debug.apk` (20.9 MB) |

---

## 2. Detalle de Pruebas Unitarias Automatizadas

### A. Validación de Credenciales Estudiantiles (`ValidateStudentCredentialsUseCaseTest`)
* **Ubicación:** `app/src/test/java/com/example/estudihambres/domain/usecase/ValidateStudentCredentialsUseCaseTest.kt`
* **Casos de prueba validados:**
  1. `validateDni returns true only for exactly 8 numeric digits`:
     - Entradas válidas: `12345678`, `70809010` ➔ `isValid = true`.
     - Entradas inválidas: `1234567` (7 dígitos), `123456789` (9 dígitos), `1234567A` (alfanumérico), `""` ➔ `isValid = false`.
  2. `validateUniversityEmail returns true for valid university domains`:
     - Entradas válidas: `alumno@unmsm.edu.pe`, `estudiante@pucp.pe`, `usuario@universidad.edu` ➔ `isValid = true`.
     - Entradas inválidas: `personal@gmail.com`, `invalid-email`, `""` ➔ `isValid = false`.
  3. `validatePassword requires at least 6 characters`:
     - Entradas válidas: `123456`, `claveSegura123` ➔ `isValid = true`.
     - Entradas inválidas: `12345`, `""` ➔ `isValid = false`.

### B. Parser OCR de Carnet Universitario (`ParseStudentCardOcrUseCaseTest`)
* **Ubicación:** `app/src/test/java/com/example/estudihambres/domain/usecase/ParseStudentCardOcrUseCaseTest.kt`
* **Casos de prueba validados:**
  1. `valid student card with SUNEDU, UNIVERSIDAD and year should be verified`:
     - Carnet oficial con "SUNEDU", "UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS" y "2026" ➔ Reconoce palabras clave, extrae año y `isVerified = true`.
  2. `card with only university and year should be verified`:
     - Carnet con nombre institucional y año 2025 ➔ `isVerified = true`.
  3. `random text without university keywords should not be verified`:
     - Boleta de supermercado común ➔ `isVerified = false` (evita fraudes).
  4. `empty text should return unverified`:
     - Texto en blanco ➔ `isVerified = false`.

### C. Radar Geográfico y Filtro de 30 km (`FilterPlacesByDistanceUseCaseTest` & `FilterCommercesByDistanceUseCaseTest`)
* **Ubicación:** 
  - `app/src/test/java/com/example/estudihambres/domain/usecase/FilterPlacesByDistanceUseCaseTest.kt`
  - `app/src/test/java/com/example/estudihambres/domain/usecase/FilterCommercesByDistanceUseCaseTest.kt`
* **Casos de prueba validados:**
  1. `radar filters out places beyond 30 km and retains at least 8 nearby perk places`:
     - Localización centro: Campus Lima (-12.0833, -77.0428).
     - Validación: Retiene >= 8 locales aliados en el radar y descarta el comercio foráneo a 65 km.
  2. `nearby places are ordered by closest distance`:
     - Valida orden ascendente de proximidad física en kilómetros.
  3. `calculateHaversineDistance returns zero when coordinates are identical`:
     - Precisión matemática del cálculo esférico.

### D. Catálogo y Filtro de Promociones (`PromotionFilteringTest`)
* **Ubicación:** `app/src/test/java/com/example/estudihambres/domain/usecase/PromotionFilteringTest.kt`
* **Casos de prueba validados:**
  1. `promotions catalog contains requested perks like Bembos, Spotify, Notion`:
     - Valida disponibilidad de las promociones de alta demanda estudiantil.
  2. `filtering by category Comida returns food perks`:
     - Valida consistencia de categorización para el carrusel de inicio.

---

## 3. Estado de Compilación e Integración

* **Comando de prueba:** `./gradlew testDebugUnitTest`
  - Estado: **`BUILD SUCCESSFUL`**
  - Tareas ejecutadas: 24 tareas
* **Comando de ensamble:** `./gradlew assembleDebug`
  - Estado: **`BUILD SUCCESSFUL`**
  - Tareas ejecutadas: 36 tareas
* **Salida de APK:**
  - Archivo: `app/build/outputs/apk/debug/app-debug.apk`
  - Tamaño: ~20.9 MB

---

## 4. Conclusión

El proyecto cumple al 100% las especificaciones de negocio y arquitectura establecidas en `PROJECT_RULES.md`. Todos los módulos y casos de uso cuentan con KDoc descriptivo y cobertura de pruebas automatizadas.
