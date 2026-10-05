# Finance AI

**Asistente Inteligente de Salud Financiera** — Hackathon G9 LATAM · Team 41
(Alura Latam + Oracle Next Education)

Dadas las transacciones de un usuario, el proyecto clasifica sus gastos en 8 categorías, determina su **perfil financiero** (Saludable / En observación / En riesgo) y genera **recomendaciones** personalizadas con modelos de ML.

---

## Arquitectura

```
┌──────────────┐   HTTP/JSON + JWT   ┌──────────────────┐   HTTP/JSON + X-API-Key   ┌──────────────────┐
│   Cliente    │ ──────────────────▶ │  backend (Java)  │ ────────────────────────▶ │ ml-service (Py)  │
│              │ ◀────────────────── │  Spring Boot     │ ◀──────────────────────── │  FastAPI          │
└──────────────┘      :8080          └────────┬─────────┘         :8000             └──────────────────┘
                                              │ JPA / Flyway
                                       ┌──────▼──────┐
                                       │  MySQL      │  :3306
                                       └─────────────┘
```

| Componente | Tecnología | Puerto | Carpeta |
|---|---|---|---|
| Backend | Spring Boot 3.5.15 · Java 17 · Maven | 8080 | `backend/` |
| ML API | FastAPI + scikit-learn | 8000 | `data-science/ml-api/` |
| Base de datos | MySQL 8 (migraciones Flyway) | 3306 | `backend/src/main/resources/db/migration/` |
| Frontend | *no implementado* | — | — |

---

## Stack

- **Backend:** Spring Boot, Spring Security (JWT), Spring Data JPA + Specifications, Flyway, Bucket4j (rate limiting), Lombok, springdoc/Swagger.
- **Machine Learning:** Python 3.11, FastAPI, scikit-learn, pandas, joblib. Modelos entrenados en notebooks (TF-IDF + LinearSVC calibrado para categorías; GradientBoosting para perfil financiero).
- **Infraestructura:** Docker Compose (local), Render + Aiven MySQL (staging).

---

## Quickstart

### Requisitos

Docker y Docker Compose.

### 1. Configurar variables de entorno

```bash
cp .env_example .env
# editar .env: completar MYSQL_PASSWORD, MYSQL_DB, JWT_SECRET y ML_API_KEY
```

`.env` está en `.gitignore`: **nunca se sube a GitHub**, sólo se publica `.env_example`.

### 2. Levantar todo

```bash
docker compose up -d --build
```

Levanta, en orden: `db-service` (espera healthcheck) → `ml-service` → `backend-service`.

### 3. Verificar

```bash
curl http://localhost:8080/health     # backend
curl http://localhost:8000/health     # ML service
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **MySQL:** `localhost:3306`

### Alternativas sin Docker

```bash
# Sólo la base de datos
docker compose up -d db-service

# Backend (desde backend/) — requiere MySQL corriendo
./mvnw spring-boot:run
./mvnw test                          # tests unitarios de seguridad y rate-limit

# ML API (desde data-science/)
pip install -r ml-api/requirements.txt
uvicorn ml-api.app:app --host 0.0.0.0 --port 8000
```

---

## Estructura del repositorio

```
├── backend/                        # API Spring Boot (paquete smart.finance.ai)
│   └── src/main/resources/db/migration/   # Migraciones Flyway V1..V5
├── data-science/
│   ├── ml-api/                     # Servicio FastAPI (app.py)
│   ├── *.ipynb                     # Notebooks: EDA, limpieza, modelos, evaluación
│   └── *.pkl / *.csv               # Artefactos y datasets
├── db/                             # Assets de base de datos
├── docker/                         # Dockerfiles (backend, ml-api)
├── docker-compose.yml              # Orquestación local
├── render.yaml                     # Staging en Render
├── CODE_WIKI.md                    # Documentación técnica completa
├── Documentacion.md                # Documentación del proyecto
└── CONTRIBUTING.md                 # Flujo de Git y convenciones
```

---

## API en resumen

Todos los endpoints `/api/v1/**` requieren `Authorization: Bearer <jwt>` (excepto `/auth` y `/health`).

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/signup` · `/auth/signin` | Registro y login (públicos, devuelven JWT) |
| GET | `/health` | Healthcheck (público) |
| POST | `/api/v1/transacciones` | Creación en lote de transacciones |
| GET | `/api/v1/transacciones` | Listado paginado con filtros |
| POST | `/api/v1/analisis-financiero` | Análisis financiero (modo híbrido: body o BD del usuario) |
| GET | `/api/v1/analisis-financiero` | Historial de análisis con filtros |
| GET/POST/PUT/DELETE | `/api/v1/usuarios` | CRUD de usuarios (sólo ADMIN) |

La documentación completa de endpoints, contratos JSON, ejemplos de prueba, modelo de datos y reglas del motor de recomendaciones está en **[CODE_WIKI.md](CODE_WIKI.md)**.

---

## Variables de entorno

| Variable | Requerida | Descripción |
|---|---|---|
| `JWT_SECRET` | ✅ | Secreto para firmar los JWT |
| `MYSQL_PASSWORD` | ✅ | Password de MySQL |
| `MYSQL_DB` | ✅ | Nombre de la base de datos |
| `ML_API_KEY` | ✅ | Header `X-API-Key` entre backend y ML service |
| `DB_URL` | ✅ (sin Docker) | JDBC URL del datasource |
| `MYSQL_USER` | ✅ (sin Docker) | Usuario de MySQL |
| `ML_SERVICE_URL` | ✅ (sin Docker) | URL del servicio ML |
| `JWT_EXPIRATION_MS` | — | Default `86400000` (24 h) |
| `ML_TIMEOUT_MS` | — | Default `5000` |
| `RATE_LIMIT_SIGNIN_MAX` / `_PERIOD` | — | Default `5` / `60` s |
| `RATE_LIMIT_SIGNUP_MAX` / `_PERIOD` | — | Default `5` / `300` s |

Con Docker Compose, `DB_URL` y `ML_SERVICE_URL` los resuelve el propio `docker-compose.yml`.

---

## Despliegue

Staging en **Render** (definido en `render.yaml`):

- `financeai-backend` — Docker, puerto 8080, healthcheck `/health`.
- `financeai-ml-service` — Python/FastAPI, puerto 10000.
- MySQL externo en **Aiven**; `SPRING_DATASOURCE_PASSWORD` y `JWT_SECRET` se configuran manualmente en el dashboard (`sync: false`), nunca en el repo.

> Si el servicio Aiven está **pausado**, su DNS deja de resolver y el deploy falla con `UnknownHostException`: encenderlo y volver a desplegar.

---

## Convenciones

- **Git:** GitHub Flow — `main` estable, trabajo en `feature/*` y `fix/*`, integración por Pull Request.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- **Backend:** Java 17, DTOs separados request/response, validaciones Jakarta con mensajes en español, respuestas de error centralizadas en `GlobalExceptionHandler`.
- **API:** rutas bajo `/api/v1/`, paginación estándar (`page` 0-indexed, `size` 10/20/30, `sort=campo,dir`).

Detalle completo en [CONTRIBUTING.md](CONTRIBUTING.md).

---
