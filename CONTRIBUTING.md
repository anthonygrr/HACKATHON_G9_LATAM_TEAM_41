# 🤝 Guía de Contribución

¡Gracias por contribuir a **HACKATHON G9 LATAM TEAM 41**!

Este documento describe el flujo de trabajo de Git adoptado por el equipo durante el desarrollo del proyecto.

---

# 📌 Flujo de Trabajo

Este proyecto utiliza la metodología **GitHub Flow**.

La única rama permanente del repositorio es:

```text
main
```

## `main`

La rama **main** contiene siempre la versión estable más reciente del proyecto.

* ❌ Nunca realices commits directamente sobre `main`.
* ✅ Todo desarrollo debe realizarse en una rama independiente.
* ✅ Todos los cambios deben integrarse mediante un **Pull Request**.

---

# 🚀 Primeros pasos

## 1. Clonar el repositorio

```bash
git clone https://github.com/No-Country-simulation/HACKATHON_G9_LATAM_TEAM_41.git
```

## 2. Entrar al proyecto

```bash
cd HACKATHON_G9_LATAM_TEAM_41
```

## 3. Verificar el repositorio remoto

```bash
git remote -v
```

Salida esperada:

```text
origin  https://github.com/No-Country-simulation/HACKATHON_G9_LATAM_TEAM_41.git (fetch)

origin  https://github.com/No-Country-simulation/HACKATHON_G9_LATAM_TEAM_41.git (push)
```

---

# 🌱 Iniciar una nueva tarea

## 1. Cambiar a la rama principal

```bash
git checkout main
```

---

## 2. Actualizar la rama principal

Antes de comenzar cualquier tarea, asegúrate de tener la última versión del proyecto.

```bash
git pull origin main
```

---

## 3. Crear una nueva rama

Cada tarea debe desarrollarse en una rama independiente creada a partir de `main`.

```bash
git checkout -b feature/nombre-funcionalidad main
```

Ejemplos:

```bash
git checkout -b feature/backend-auth main
```

```bash
git checkout -b feature/frontend-dashboard main
```

```bash
git checkout -b feature/devops-docker main
```

Para corrección de errores:

```bash
git checkout -b fix/login-validation main
```

---

# 💻 Desarrollar la funcionalidad

Realiza los cambios necesarios.

Se recomienda realizar commits pequeños y frecuentes.

---

# 🔍 Revisar los cambios

Verifica los archivos modificados:

```bash
git status
```

Visualiza las diferencias:

```bash
git diff
```

---

# 📦 Preparar los cambios

Agregar todos los archivos:

```bash
git add .
```

O agregar un archivo específico:

```bash
git add src/components/Login.tsx
```

---

# ✅ Realizar el commit

Seguimos la especificación **Conventional Commits**.

### Nueva funcionalidad

```bash
git commit -m "feat: implementar autenticación JWT"
```

### Corrección de errores

```bash
git commit -m "fix: validar credenciales de inicio de sesión"
```

### Documentación

```bash
git commit -m "docs: actualizar documentación de la API"
```

### Refactorización

```bash
git commit -m "refactor: simplificar servicio de autenticación"
```

### Pruebas

```bash
git commit -m "test: agregar pruebas del servicio de autenticación"
```

### Configuración o mantenimiento

```bash
git commit -m "chore: configurar Docker Compose"
```

---

# ⬆️ Subir la rama a GitHub

Primer envío:

```bash
git push -u origin feature/backend-auth
```

Envíos posteriores:

```bash
git push
```

---

# 🔄 Crear un Pull Request

Una vez finalizada la tarea:

1. Ingresa al repositorio en GitHub.
2. Haz clic en **Compare & Pull Request**.
3. Crea un Pull Request con destino a la rama **main**.
4. Solicita revisión de código.
5. Atiende las observaciones del revisor si existen.
6. Fusiona el Pull Request únicamente después de ser aprobado.

---

# 📝 Convención para Pull Requests

## Título

Debe seguir la especificación **Conventional Commits**.

Ejemplo:

```text
feat: implementar autenticación JWT
```

---

## Descripción

```markdown
## Resumen

- Se implementó la autenticación mediante JWT.
- Se creó el endpoint de inicio de sesión.
- Se configuró Spring Security.
- Se agregaron pruebas unitarias.

Closes #12
```

---

# 📦 Convención de mensajes de commit

| Tipo     | Cuándo utilizarlo                                |
| -------- | ------------------------------------------------ |
| feat     | Nueva funcionalidad                              |
| fix      | Corrección de errores                            |
| docs     | Documentación                                    |
| refactor | Refactorización de código                        |
| style    | Cambios de formato                               |
| test     | Pruebas                                          |
| chore    | Configuración o mantenimiento                    |
| perf     | Mejoras de rendimiento                           |
| ci       | Configuración de integración continua            |
| build    | Cambios en dependencias o sistema de compilación |

---

# 🌿 Convención para nombrar ramas

Utiliza nombres descriptivos.

### Nuevas funcionalidades

```text
feature/backend-auth

feature/frontend-dashboard

feature/user-profile
```

### Corrección de errores

```text
fix/login-validation

fix/navbar-overflow
```

### Documentación

```text
docs/update-readme
```

### Refactorización

```text
refactor/auth-service
```

### DevOps

```text
chore/docker-compose

ci/github-actions
```

---

# 🚫 Evita

Nunca utilices mensajes como:

```text
update

test

fix

cambios

proyecto

final

commit
```

Estos mensajes no describen claramente qué se modificó.

---

# ✅ Prefiere

```text
feat: agregar endpoint para registro de pacientes

fix: evitar registros con correo duplicado

docs: actualizar guía de despliegue

refactor: simplificar servicio de usuarios

ci: agregar workflow para Backend

chore: configurar entorno Docker
```

---

# 🔄 Flujo de GitHub Flow

```text
Actualizar main

↓

Crear una nueva rama

↓

Desarrollar la funcionalidad

↓

Realizar commits

↓

Subir la rama

↓

Crear Pull Request

↓

Revisión de código

↓

Fusionar en main

↓

Eliminar la rama
```

---

# 📋 Buenas prácticas

* Realiza commits pequeños y frecuentes.
* Escribe mensajes de commit claros y descriptivos.
* Nunca trabajes directamente sobre `main`.
* Actualiza tu rama `main` antes de iniciar una nueva tarea.
* Mantén tu rama sincronizada con `main` si el Pull Request tarda en ser aprobado.
* Solicita revisión antes de fusionar tus cambios.
* Elimina la rama una vez que el Pull Request haya sido fusionado.

---

# 💡 Consejo

Un buen mensaje de commit responde a la pregunta:

> **¿Qué se hizo?**

Buen ejemplo:

```text
feat: agregar endpoint para búsqueda de pacientes
```

Mal ejemplo:

```text
arreglé todo
```

Si otro desarrollador puede entender el cambio únicamente leyendo el historial de commits, entonces el mensaje está bien escrito.
