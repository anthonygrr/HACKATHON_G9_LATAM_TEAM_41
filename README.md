# HACKATHON_G9_LATAM_TEAM_41
Proyecto Finance AI – Asistente Inteligente de Salud Financiera

# Finance AI — Documentación Técnica del Proyecto

> **CODE-WIKI** · Hackathon G9 LATAM Team 41
> Documentación oficial de referencia técnica del backend, el servicio de ML y la infraestructura.

---

## Índice

1. [¿Qué es el proyecto?](#1-qué-es-el-proyecto)
2. [Backend](#2-backend-backend)
3. [Data Science / ML](#3-data-science--ml-data-science)
4. [Base de datos](#4-base-de-datos-flyway-backendsrcmainresourcesdbmigration)
5. [Docker](#5-docker)
6. [Despliegue en Render](#6-despliegue-en-render-staging-renderyaml)
7. [Variables de entorno](#7-variables-de-entorno-env--env_example)
8. [Comandos útiles](#8-comandos-útiles)
9. [Convenciones del repo](#9-convenciones-del-repo)
10. [Cambios recientes](#10-cambios-recientes)
11. [Mejoras futuras / notas](#11-mejoras-futuras--notas)
12. [Ejemplos de prueba (JSON)](#12-ejemplos-de-prueba-json)

---

## 1. ¿Qué es el proyecto?

**Finance AI – Asistente Inteligente de Salud Financiera** (Hackathon G9 LATAM Team 41).

Un asistente que, dadas las transacciones de un usuario, clasifica sus gastos, determina su **perfil financiero** (Saludable / En observación / En riesgo) y genera **recomendaciones** personalizadas usando ML.

### Arquitectura (3 componentes)

```
┌──────────────┐   HTTP/JSON + JWT   ┌──────────────────┐   HTTP/JSON + X-API-Key   ┌──────────────────┐
│   Cliente    │ ──────────────────▶ │  backend (Java)  │ ────────────────────────▶ │ ml-service (Py)  │
│  (React/POST)│ ◀────────────────── │  Spring Boot     │ ◀──────────────────────── │  FastAPI          │
└──────────────┘   puerto 8080       │  puerto 8080     │      puerto 8000          └──────────────────┘
                                     └────────┬─────────┘
                                              │ JPA / Flyway
                                     ┌────────▼─────────┐
                                     │  MySQL (3306)     │
                                     └──────────────────┘
```

| Componente | Tecnología | Puerto | Carpeta |
|---|---|---|---|
| Backend | Spring Boot 3.5.15 (Java 17, Maven) | 8080 | `backend/` |
| ML API | FastAPI + scikit-learn | 8000 | `data-science/ml-api/` |
| BD | MySQL | 3306 | gestionada por Flyway |
| Frontend | No implementado (comentado en compose) | — | — |

---

## 2. Backend (`backend/`)

- **Paquete base:** `smart.finance.ai`
- **App principal:** `SmartAssistantFinanceAiLatamG9Team41Application.java`
- **Artefacto Maven:** `smart.finance.ai:SmartAssistantFinanceAILatamG9Team41:0.0.1-SNAPSHOT`

### Estructura de paquetes

| Paquete | Contenido |
|---|---|
| `controller/` | 6 REST controllers (Auth, Usuario, Transaccion, AnalisisFinanciero, Home, Health) |
| `service/` | Interfaces + `*Impl` + `CustomUserDetailsService` + `MlApiClient` |
| `repository/` | 10 repositorios Spring Data JPA (Transaccion, AnalisisFinanciero y Usuario extienden `JpaSpecificationExecutor`) |
| `entity/` | 10 entidades Lombok |
| `dto/` | `common/` (`PageResponseDTO`), `request/`, `response/`, `ml/` (DTOs de la API de ML con snake_case) |
| `specification/` | 3 Specifications de filtrado dinámico (Transaccion, AnalisisFinanciero, Usuario) |
| `config/` | `OpenApiConfig`, `ml/MlClientConfig`, `security/*` (JWT, rate limit, `@EnableMethodSecurity`) |
| `exception/` | `GlobalExceptionHandler` (@RestControllerAdvice) + excepciones 404/403/409 |
| `util/` | `CategoriaGastoMapper` (slugs ML ↔ IDs de BD), `PaginacionUtils` (valida page/size/sort) |

### Endpoints

#### Auth — `/auth` (público)
| Método | Ruta | Notas |
|---|---|---|
| POST | `/auth/signup` | Registro, rol `ROLE_USER`, devuelve JWT (201) |
| POST | `/auth/signin` | Login BCrypt, devuelve JWT + rol (200) |

#### Usuarios — `/api/v1/usuarios` (solo ADMIN)
| Método | Ruta | Notas |
|---|---|---|
| POST | `/api/v1/usuarios` | Crear usuario (admin; 201) |
| GET | `/api/v1/usuarios?page=&size=&sort=` | Listar paginado + filtros `nombre`, `correo`, `fechaNacimientoInicio`, `fechaNacimientoFin` |
| GET | `/api/v1/usuarios/{id}` | Detalle |
| PUT | `/api/v1/usuarios/{id}` | Incluye cambio de rol |
| DELETE | `/api/v1/usuarios/{id}` | (204) |

> Validaciones de `UsuarioUpdateRequest`: nombre y apellido paterno obligatorios (2–100), correo obligatorio con formato `@Email`, `rolId` obligatorio. La validación de correo duplicado **excluye al propio usuario** (evita falso 409 al editar sin cambiar correo). El listado exige `@PreAuthorize("hasRole('ADMIN')")` más chequeo equivalente en servicio.

#### Transacciones — `/api/v1/transacciones` (USER o ADMIN)
| Método | Ruta | Notas |
|---|---|---|
| POST | `/api/v1/transacciones` | Creación en lote (201). Campo `fecha` **opcional** (`yyyy-MM-dd`): si se omite se usa la fecha del sistema |
| GET | `/api/v1/transacciones?usuarioId=` | Paginado + filtros `descripcion`, `tipo`, `fechaInicio`, `fechaFin`, `page`, `size`, `sort`; USER ve solo las suyas |
| GET/PUT/DELETE | `/api/v1/transacciones/{id}` | Solo dueño o admin (403 si no) |

> Admin sin `usuarioId` ve TODAS las transacciones; USER siempre acotado a las propias (anti-IDOR). Sort por defecto: `id` asc.
>
> **Categorización:** los gastos se clasifican en 8 categorías (ML o reglas locales). Las transacciones tipo `INGRESO` **no se categorizan**: `categoria`, `idCategoria` y `probabilidad` llegan `null` en el response.

#### Análisis financiero — `/api/v1/analisis-financiero` (USER o ADMIN)
| Método | Ruta | Notas |
|---|---|---|
| POST | `/api/v1/analisis-financiero` | **Modo híbrido:** con `transacciones` en body las usa (simulación); sin body o con lista vacía consulta las transacciones persistidas del usuario autenticado y las envía al ML (201). Si el usuario no tiene transacciones → 400 |
| GET | `/api/v1/analisis-financiero?usuarioId=` | Historial paginado + filtros `salud`, `fechaInicio`, `fechaFin`, `page`, `size`, `sort` |
| GET | `/api/v1/analisis-financiero/{id}` | Detalle (resúmenes + recomendaciones) |
| PUT | `/api/v1/analisis-financiero/{id}` | Dueño o admin. **Exige** `transacciones` explícitas (no recalcula desde BD); lista vacía → 400 |
| DELETE | `/api/v1/analisis-financiero/{id}` | Dueño o admin (204) |

> En modo simulación las transacciones recibidas se persisten por trazabilidad; en modo BD **no** se duplican.
>
> Paginación unificada: `page` 0-indexed, `size` solo 10/20/30 (default 10), `sort=campo,dir` con whitelist por entidad (campo inválido → default). Implementado en `PaginacionUtils`.

#### Home — `/api/v1/home`
| Método | Ruta | Notas |
|---|---|---|
| GET | `/api/v1/home` | `{"msg": "Welcome"}` (autenticado) |

#### Health — `/health` (público)
| Método | Ruta | Notas |
|---|---|---|
| GET | `/health` | Healthcheck para Render/balanceadores |

### Seguridad

- **JWT** (jjwt 0.13.0): token con claims `email`, `uid`, `authorities`, `jti`; expiración 24h por defecto. Header `Authorization: Bearer <token>`.
- **Roles:** `ROLE_ADMIN`, `ROLE_USER`. `/api/v1/usuarios` GET/POST → solo admin (vía `@PreAuthorize` + chequeo en servicio); `/api/v1/**` → USER o ADMIN; `/health` público; resto `permitAll`.
- **Stateless**, CSRF off. **CORS:** `http://localhost:5173` y `http://localhost:3000`.
- **Method Security:** `@EnableMethodSecurity` activado en `SecurityConfig` (requerido para que `@PreAuthorize` funcione).
- **Rate limiting** (Bucket4j, en memoria por IP): signin 5 req/60s, signup 5 req/300s; exceso → **429** + `Retry-After`.
- **Autorización por dueño:** `SecurityUtils` (`currentUserId()`, `isAdmin()`, `effectiveUserId()`, `resolveOwnerOrAdmin()`, `assertOwnerOrAdmin()`) — admin ve todo (o filtra por `usuarioId` explícito), USER solo sus recursos.
- **Passwords:** `BCryptPasswordEncoder`.

### Conexión con ML

- Bean `RestClient` configurado en `config/ml/MlClientConfig.java` (base URL, header `X-API-Key`, timeout 5s).
- `service/MlApiClient.java` hace 3 llamadas con **fallback a lógica local** si el servicio cae:
  - `POST /clasificar-transaccion`
  - `POST /clasificar-transacciones` (lote)
  - `POST /analisis-financiero`
- Fallbacks: `ClasificacionServiceImpl` usa reglas por palabras clave (prob. 0.87); `AnalisisFinancieroServiceImpl.poblarPorRegla()` usa clasificación local y salud derivada de probabilidad (≥0.80 SALUDABLE, ≥0.65 MODERADA, si no EN_RIESGO).
- Mapeo slugs↔IDs en `util/CategoriaGastoMapper.java`.

### Configuración clave (`src/main/resources/application.properties`)

- Carga `.env` desde la raíz del repo (`springdotenv.directory=../`; funciona tanto desde IntelliJ como desde `backend/`).
- `spring.datasource.*` ← vars `DB_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`.
- `spring.jpa.hibernate.ddl-auto=validate` (esquema controlado por Flyway).
- `jwt.secret` ← `JWT_SECRET`; `jwt.expiration-ms` ← `JWT_EXPIRATION_MS` (default 86400000).
- `ml.service.base-url` ← `ML_SERVICE_URL`; `ml.service.api-key` ← `ML_API_KEY`; `ml.service.timeout-ms` ← `ML_TIMEOUT_MS` (5000).
- Rate limit configurable por env `RATE_LIMIT_*`.

### Swagger

Disponible en `/swagger-ui.html` (springdoc 2.8.6) con scheme `bearerAuth` JWT. Los DTOs usan anotaciones `@Schema` / `@Operation` / `@Parameter` en español para documentar campos opcionales y filtros.

---

## 3. Data Science / ML (`data-science/`)

### Pipeline (pasos documentados en los notebooks)

```
generar_dataset_financeai.py ─▶ EDA ─▶ limpieza/ingeniería ─▶ 2 modelos ─▶ motor reglas ─▶ evaluación ─▶ ml-api
```

| Paso | Archivo | Propósito |
|---|---|---|
| 1 | `generar_dataset_financeai.py` | Genera datasets sintéticos (seed 42, 800 usuarios, 13,633 transacciones) |
| 2 | `eda_financeai5.ipynb` | Análisis exploratorio de los datasets |
| 3 | `limpieza_ingenieria_atributos.ipynb` | Limpieza de texto, capping IQR, features, split 80/20, serializa artefactos |
| 5 | `modelo_clasificador_gastos.ipynb` | Clasificador de gastos (texto → 8 categorías) |
| 6 | `modelo_clasificador_perfil.ipynb` | Clasificador de perfil (features → 3 perfiles) |
| 7 | `motor_recomendaciones.ipynb` | Motor de reglas (sin ML) → `reglas_recomendaciones.json` |
| 8 | `evaluacion_modelos.ipynb` | Evaluación consolidada → `reporte_evaluacion_final.{json,csv}` |

### Modelos (artefactos `.pkl` en raíz de `data-science/`)

| Artefacto | Modelo | Métricas clave |
|---|---|---|
| `modelo_clasificador_gastos.pkl` | `CalibratedClassifierCV(LinearSVC)` + TF-IDF | acc 89.84%, F1 91.01%, AUC 0.9916 |
| `modelo_perfil_financiero.pkl` | `GradientBoostingClassifier` | acc 93.75%, F1 93.83%, AUC 0.9665 |
| `tfidf_vectorizer.pkl` | `TfidfVectorizer` | Vocabulario ajustado solo en train |
| `scaler_perfil.pkl` | `StandardScaler` | No usado por el modelo de perfil (`requiere_escalado: false`) |
| `encoder_frecuencia_ahorro.pkl` | `OrdinalEncoder` (Baja/Media/Alta) | Codifica frecuencia de ahorro |

**Archivos de metadatos:** `metadata_modelo_perfil.json` (31 features, 22 continuas), `metadata_modelo_gastos.json`, `columnas_features_perfil.json`, `reglas_recomendaciones.json` (umbrales, `max_recomendaciones: 4`), `reporte_evaluacion_final.json/.csv`.

**8 categorías de gasto:** Alimentación, Transporte, Salud, Vivienda, Educación, Ocio, Servicios, Otras.

### ML API (`ml-api/app.py`, FastAPI)

Carga artefactos desde `MODEL_DIR` (env). Preprocesa igual que los notebooks (quita tildes, `REF#`, `##`, ciudades).

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/health` | Healthcheck Docker |
| POST | `/clasificar-transaccion` | Clasifica una descripción → categoría + probabilidad |
| POST | `/clasificar-transacciones` | Lote de clasificaciones |
| GET | `/clasificar?descripcion=` | Debug |
| POST | `/analisis-financiero` | Análisis completo → perfil + resumen + recomendaciones |

**Dependencias** (`requirements.txt`): fastapi 0.115.6, uvicorn 0.34.0, pydantic 2.10.4, joblib 1.4.2, numpy 2.3.1, pandas 2.2.3, scikit-learn 1.8.0.

### Reglas de negocio del motor de recomendaciones (`reglas_recomendaciones.json`)

Las recomendaciones se generan por reglas (sin ML) en `generar_recomendaciones()` de `ml-api/app.py`:

- **Alerta por categoría** (`umbral_pct_categoria`, pct del gasto sobre el ingreso): vivienda >35%, alimentación >25%, transporte >15%, ocio >10%, servicios >10%, otros >15%. Salud y Educación **no** tienen umbral (gastar más ahí no se considera negativo).
- **Severidad:** 3 = alta, 2 = media, 1 = informativa. Una categoría con pct > 1.3× su umbral sube de severidad (1→2).
- **Ratio gasto/ingreso:** >90% → severidad 3 ("gastando casi todo o más de lo que ganas"); >70% → severidad 2.
- **Nivel de endeudamiento (%):** >50 → severidad 3; >30 → severidad 1.
- **Frecuencia de ahorro:** "Baja" → severidad 2; "Media" → severidad 1; "Alta" sin alerta.
- **Salida:** ordenadas por severidad desc, limitadas a `max_recomendaciones: 4`, y **siempre incluyen primero el mensaje general del perfil** (`mensajes_perfil_general`: Saludable / En observacion / En riesgo).

### Datasets CSV

| Archivo | Shape | Uso |
|---|---|---|
| `dataset_transacciones.csv` | 13,633×5 | Crudo transacciones |
| `dataset_usuarios_perfil.csv` | 800×16 | Crudo usuarios |
| `train/test_transacciones.csv` | 10,832 / 2,708 | Split clasificador gastos (texto limpio) |
| `train/test_usuarios.csv` | 640 / 160 | Split perfil (31 features) |
| `train/test_usuarios_escalado.csv` | 640 / 160 | Split perfil escalado |

### Limitaciones conocidas (documentadas en los notebooks)

- Vocabulario TF-IDF desconocido (p. ej. "Gimnasio") → cae en clase por defecto (Alimentación).
- Colisión "Farmacias Guadalajara" (comercio) vs ciudad "Guadalajara".
- "Restaurante" clasifica como Ocio por diseño del catálogo.
- Datos sintéticos con ruido deliberado (12% descripciones genéricas, 10% typos, 7% ruido de etiqueta).

---

## 4. Base de datos (Flyway, `backend/src/main/resources/db/migration/`)

| Migración | Contenido |
|---|---|
| `V1__create_schema.sql` | 10 tablas: `rol`, `usuario`, `tipo_transaccion`, `salud_financiera`, `categoria_gasto`, `transaccion`, `analisis_financiero`, `clasificacion_transaccion` (1:1), `resumen_gasto`, `recomendacion` |
| `V2__insert_initial_catalogs.sql` | Catálogos: roles, tipos (INGRESO/GASTO), salud (SALUDABLE/MODERADA/EN_RIESGO), 8 categorías |
| `V3__insert_sample_data.sql` | 9 usuarios demo + transacciones de ejemplo (2026) |
| `V4__insert_financial_analysis_data.sql` | 35 análisis + clasificaciones + resúmenes + recomendaciones |
| `V5__promote_sample_admin.sql` | Promueve `ana.martinez@example.com` a ADMIN |

Tablas en `snake_case`; `ddl-auto=validate`; índices en `transaccion(usuario,fecha)` y `analisis_financiero(usuario,anio,mes)`.

---

## 5. Docker

`docker-compose.yml` (red `smart-finance-ai-network`, red bridge):

```bash
docker compose up -d db-service      # MySQL, healthcheck via mysqladmin ping
docker compose up -d ml-service      # FastAPI en 8000
docker compose up -d backend-service # Spring Boot en 8080 (depende de db healthy + ml started)
```

- `docker/ml-api.Dockerfile`: python 3.11-slim, uvicorn `ml-api.app:app`, `MODEL_DIR=/app/data-science`.
- `docker/backend.Dockerfile`: build multi-stage Maven 3.9.6 + Temurin 17 → JRE alpine.
- Env del backend en compose: `DB_URL`, `MYSQL_USER/PASSWORD`, `JWT_SECRET`, `ML_SERVICE_URL=http://ml-service:8000`, `ML_API_KEY`.
- Frontend: **comentado** (pendiente).

---

## 6. Despliegue en Render (staging, `render.yaml`)

- **Backend** `financeai-backend` (Docker, Java 17): puerto 8080, healthcheck `/health`, datasource vía `SPRING_DATASOURCE_URL` a MySQL **externo en Aiven** (no gestionado por Render).
- **ML service** `financeai-ml-service` (Python/FastAPI): puerto 10000, `MODEL_DIR=..` (apunta a `data-science/`), healthcheck `/health`.
- **MySQL en Aiven:** servicio `finance-db` → host `finance-db-mysql-finance-ai-project.k.aivencloud.com:28083`, BD `defaultdb`, SSL requerido.
- **Secretos en Render:** `SPRING_DATASOURCE_PASSWORD` y `JWT_SECRET` son `sync: false` (se configuran manualmente en el dashboard; nunca en el repo).
- **Gotcha operativo:** si el servicio Aiven está **pausado**, su DNS deja de resolver (`UnknownHostException` en el deploy); encenderlo (REBUILDING → RUNNING) y volver a desplegar.

---

## 7. Variables de entorno (`.env` / `.env_example`)

```
JWT_SECRET=
MYSQL_USER=
MYSQL_PASSWORD=
MYSQL_DB=
DB_URL=
ML_SERVICE_URL=
ML_API_KEY=
```

Opcionales (con defaults): `JWT_EXPIRATION_MS=86400000`, `JWT_ISSUER=finance-ai`, `ML_TIMEOUT_MS=5000`, `RATE_LIMIT_SIGNIN_MAX=5`, `RATE_LIMIT_SIGNIN_PERIOD=60`, `RATE_LIMIT_SIGNUP_MAX=5`, `RATE_LIMIT_SIGNUP_PERIOD=300`.

Hay `.env` tanto en la raíz como en `backend/` (idéntico contenido, para levantar desde IntelliJ o Maven).

---

## 8. Comandos útiles

```bash
# Backend local (desde backend/)
./mvnw spring-boot:run
./mvnw test                     # 8 tests unitarios (SecurityUtilsTest, RateLimitFilterTest); el test de contexto requiere MySQL activo
./mvnw clean package -DskipTests

# ML API local
pip install -r data-science/ml-api/requirements.txt
uvicorn ml-api.app:app --host 0.0.0.0 --port 8000   # desde data-science/

# Full stack local
docker compose up -d --build

# Swagger
# http://localhost:8080/swagger-ui.html
```

---

## 9. Convenciones del repo

- **Git:** GitHub Flow — rama `main` estable, trabajar en ramas `feature/*`, `fix/*`, integración por Pull Request. Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`). Detalle completo en `CONTRIBUTING.md`.
- **Backend:** Java 17, Spring Boot, Lombok, `snake_case` en BD, DTOs separados request/response, validaciones con Jakarta Validation y mensajes en español.
- **API REST:** rutas bajo `/api/v1/`, respuestas de error centralizadas en `GlobalExceptionHandler`, paginación estándar `PageResponseDTO`.
- **ML:** artefactos serializados con joblib, cargados vía `MODEL_DIR`.

---

## 10. Cambios recientes

| Fecha aprox. | Cambio | Integración |
|---|---|---|
| 2026-08 | Paginación dinámica + filtrado en transacciones, análisis y usuarios (`PageResponseDTO`, Specifications, `PaginacionUtils`, `@EnableMethodSecurity`) | PRs #14/#16 |
| 2026-08 | CRUD admin de usuarios movido a `/api/v1/usuarios` (crear/listar/editar/borrar) | PR #13/#16 |
| 2026-08 | Ingresos sin categoría: `categoria`, `idCategoria` y `probabilidad` en `null` cuando el tipo es INGRESO | PR #18 |
| 2026-08 | Fecha opcional al crear transacciones (`yyyy-MM-dd`, fallback fecha del sistema) | commit `b0d9ba6` |
| 2026-08 | Análisis financiero híbrido: POST acepta body sin `transacciones` (usa BD del usuario autenticado); PUT mantiene lista obligatoria | commit `b0d9ba6` |
| 2026-08 | Deploy staging en Render + MySQL externo Aiven; healthchecks `/health` | `render.yaml` |

---

## 11. Mejoras futuras / notas

- Frontend aún no implementado (comentado en docker-compose).
- Paso 9 del pipeline (integración con OCI Object Storage) mencionado como pendiente en notebooks.
- Los notebooks referencian una subcarpeta `artefactos/` que hoy no existe (los archivos están en la raíz de `data-science/`).
- Protección anti-lockout: un admin puede **borrarse a sí mismo, degradarse o borrar al último admin** (falta validación: no auto-demote/delete y mantener al menos 1 admin vigente).
- Contrato de error mejorable: `ErrorResponse` sin campo `code`; `BadCredentialsException` en login devuelve 500 en vez de 401.
- Matchers legacy `/usuarios`, `/usuarios/**` siguen presentes en `SecurityConfig` además de `/api/v1/usuarios` (candidatos a limpieza).
- Cobertura de tests: solo unitarias de seguridad/rate-limit; falta cobertura de servicios y controladores (incluyendo modo híbrido del análisis y fecha opcional).

---

## 12. Ejemplos de prueba (JSON)

> Todos los endpoints `/api/v1/**` requieren el header `Authorization: Bearer <jwt>` (salvo `/auth` y `/health`).
> Las fechas usan `yyyy-MM-dd`; los montos son números (no strings).
> (De acuerdo a `SignupRequest`, `LoginRequest`, `AnalisisFinancieroRequest`, `TransaccionRequestDTO` y DTOs de respuesta.)

### 12.1 Crear cuenta — `POST /auth/signup` (201)
```json
POST /auth/signup
{
  "nombre": "Daniela",
  "apellidoPaterno": "Carrillo",
  "apellidoMaterno": "Lopez",
  "fechaNacimiento": "1998-04-12",
  "correo": "daniela.carrillo@example.com",
  "contrasena": "secret123"
}
```
**Respuesta:**
```json
{
  "jwt": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Usuario registrado exitosamente",
  "rol": "ROLE_USER"
}
```

### 12.2 Inicio de sesión — `POST /auth/signin`
**Usuario normal (200):**
```json
POST /auth/signin
{ "correo": "daniela.carrillo@example.com", "contrasena": "secret123" }
```
**Admin (200):** mismo body, credenciales de admin (ej. `ana.martinez@example.com`, promovida en `V5__promote_sample_admin.sql`).
```json
POST /auth/signin
{ "correo": "ana.martinez@example.com", "contrasena": "adminpass" }
```
**Respuesta (ambos):**
```json
{
  "jwt": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Inicio de sesión exitoso",
  "rol": "ROLE_ADMIN"
}
```

### 12.3 Crear análisis — `POST /api/v1/analisis-financiero` (201)
```json
POST /api/v1/analisis-financiero
Authorization: Bearer <jwt>
{
  "usuarioId": 9,
  "ingresoMensual": 6000.00,
  "nivelEndeudamiento": 30,
  "frecuenciaAhorro": "Alta",
  "mes": 9,
  "anio": 2026,
  "fechaGeneracion": "2026-08-21",
  "saludFinanciera": "MODERADA",
  "transacciones": [
    { "descripcion": "supermercado walmart", "monto": 1200.50 },
    { "descripcion": "netflix mensual", "monto": 299.00 },
    { "descripcion": "uber viaje aeropuerto", "monto": 350.00 },
    { "descripcion": "colegiatura hijo", "monto": 2500.00 }
  ]
}
```
**Respuesta (`AnalisisFinancieroResponse`):**
```json
{
  "id": 40,
  "usuarioId": 9,
  "ingresoMensual": 6000.00,
  "nivelEndeudamiento": 30,
  "frecuenciaAhorro": "Alta",
  "mes": 9,
  "anio": 2026,
  "fechaGeneracion": "2026-08-21T12:00:00",
  "saludFinanciera": "MODERADA",
  "probabilidad": 0.720,
  "resumenGastos": [
    { "categoria": "Educación", "idCategoria": 5, "montoTotal": 2500.00, "probabilidad": 0.91 },
    { "categoria": "Alimentación", "idCategoria": 1, "montoTotal": 1200.50, "probabilidad": 0.88 }
  ],
  "recomendaciones": [
    "Ajustar el presupuesto en las categorías de mayor gasto",
    "Destinar al menos un 10% del ingreso al ahorro mensual"
  ]
}
```

### 12.4 Re-procesar análisis — `PUT /api/v1/analisis-financiero/{id}` (200)
Reutiliza el registro existente (no crea fila duplicada; ver bug de UK en historial de cambios).
```json
PUT /api/v1/analisis-financiero/40
Authorization: Bearer <jwt>
{
  "usuarioId": 9,
  "ingresoMensual": 6500.00,
  "nivelEndeudamiento": 25,
  "frecuenciaAhorro": "Alta",
  "mes": 9,
  "anio": 2026,
  "fechaGeneracion": "2026-08-22",
  "saludFinanciera": "SALUDABLE",
  "transacciones": [
    { "descripcion": "supermercado walmart", "monto": 1100.00 },
    { "descripcion": "netflix mensual", "monto": 299.00 },
    { "descripcion": "pago nomina", "monto": 15000.00 }
  ]
}
```
**Respuesta:** misma estructura que 12.3 con el `id` preservado (`40`) y los nuevos valores.

### 12.5 Crear transacciones en lote — `POST /api/v1/transacciones` (201)
```json
POST /api/v1/transacciones
Authorization: Bearer <jwt>
[
  { "descripcion": "supermercado walmart", "monto": 1200.50 },
  { "descripcion": "pago nomina", "monto": 15000.00, "tipoTransaccionId": 1 },
  { "descripcion": "uber viaje aeropuerto", "monto": 350.00, "fecha": "2026-08-20" }
]
```
**Respuesta (`List<TransaccionResponseDTO>`):** cada item incluye `id`, `categoria`, `idCategoria`, `probabilidad` (estos 3 en `null` si el tipo es INGRESO), `tipoTransaccion` ("INGRESO"/"GASTO") y `usuarioId`.

### 12.6 Listar transacciones — `GET /api/v1/transacciones` (200)
```
GET /api/v1/transacciones?tipo=GASTO&descripcion=uber&page=0&size=10&sort=fecha,desc
Authorization: Bearer <jwt>
```
**Respuesta (`PageResponseDTO<TransaccionResponseDTO>`):**
```json
{
  "content": [
    { "id": 12, "descripcion": "uber viaje aeropuerto", "categoria": "Transporte",
      "idCategoria": 2, "monto": 350.00, "fecha": "2026-08-20",
      "tipoTransaccion": "GASTO", "probabilidad": 0.86, "usuarioId": 9 }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### 12.7 Respuestas de error — `ErrorResponse`
Contrato estándar (`GlobalExceptionHandler`): `timestamp`, `status`, `error`, `message`, `path`.

**401 — No autenticado / token inválido:**
```json
{ "timestamp": "2026-08-22T04:00:00", "status": 401, "error": "Unauthorized",
  "message": "Token JWT inválido o expirado", "path": "/api/v1/analisis-financiero" }
```
**403 — Sin permiso (no es dueño ni admin):**
```json
{ "timestamp": "2026-08-22T04:01:00", "status": 403, "error": "Forbidden",
  "message": "El análisis financiero no te pertenece.", "path": "/api/v1/analisis-financiero/40" }
```
**400 — Validación / lista vacía en PUT:**
```json
{ "timestamp": "2026-08-22T04:02:00", "status": 400, "error": "Bad Request",
  "message": "Debe proporcionar la lista de transacciones al actualizar un análisis financiero.",
  "path": "/api/v1/analisis-financiero/40" }
```
**409 — Correo duplicado en signup:**
```json
{ "timestamp": "2026-08-22T04:03:00", "status": 409, "error": "Conflict",
  "message": "El correo ya está registrado", "path": "/auth/signup" }
```
**429 — Rate limit excedido (signup/signin):**
```json
{ "timestamp": "2026-08-22T04:04:00", "status": 429, "error": "Too Many Requests",
  "message": "Demasiadas solicitudes. Intente más tarde.", "path": "/auth/signin" }
```
