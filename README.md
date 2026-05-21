# Cronos — Sistema de Gestión Tributaria

Aplicación web para la gestión integral de obligaciones tributarias de
contribuyentes en Colombia. Incluye panel de administración para
**gerentes** y **asesores**, y un **portal del contribuyente** para
que los clientes consulten su información y suban documentos.

---

## Tabla de contenidos

1. [Stack tecnológico](#stack-tecnológico)
2. [Arquitectura general](#arquitectura-general)
3. [Estructura del proyecto](#estructura-del-proyecto)
4. [Modelo de datos](#modelo-de-datos)
5. [Roles y seguridad](#roles-y-seguridad)
6. [Módulos implementados](#módulos-implementados)
7. [API REST](#api-rest)
8. [Vistas (Thymeleaf)](#vistas-thymeleaf)
9. [Configuración](#configuración)
10. [Cómo levantar el proyecto](#cómo-levantar-el-proyecto)
11. [Tests](#tests)
12. [Credenciales por defecto](#credenciales-por-defecto)

---

## Stack tecnológico

| Capa            | Tecnología                                      |
|-----------------|-------------------------------------------------|
| Lenguaje        | Java 17                                          |
| Framework       | Spring Boot 4.0.6 (Spring 7)                     |
| Persistencia    | MongoDB Atlas (Spring Data MongoDB 5)            |
| Vistas          | Thymeleaf 3.1 + Spring Security extras           |
| Seguridad       | Spring Security 7 (BCrypt + sesión por form)     |
| Validación      | Jakarta Bean Validation (Hibernate Validator 9)  |
| Correo          | Spring Boot Mail (SMTP — Gmail por defecto)      |
| Build           | Maven 3 (con `mvnw` wrapper)                     |
| Tests           | JUnit 5 + Mockito 5 + Spring Test (MockMvc)      |

> No se usa Bootstrap. El CSS es propio del proyecto (`src/main/resources/static/css/app.css`).

---

## Arquitectura general

Aplicación monolítica Spring Boot con dos zonas claramente separadas:

```
                ┌──────────────────────────────────────────────┐
                │              Spring Security                │
                │   (formLogin, BCrypt, @PreAuthorize, CSRF)   │
                └──────────────────────────────────────────────┘
                                     │
       ┌─────────────────────────────┼─────────────────────────────┐
       │                             │                             │
┌──────▼─────────┐         ┌─────────▼─────────┐         ┌─────────▼────────┐
│  Panel Admin   │         │  Portal del       │         │   API REST       │
│ (GERENTE/      │         │  Contribuyente    │         │ (/api/**)        │
│  ASESOR)       │         │ (/portal/**)      │         │                  │
│                │         │                   │         │                  │
│ /dashboard     │         │ /portal/inicio    │         │ /api/contribu-   │
│ /clientes      │         │ /portal/oblig…    │         │   yente/**       │
│ /obligaciones  │         │ /portal/docum…    │         │ /api/oblig…      │
│ /empleados     │         │                   │         │                  │
│ /reportes      │         │                   │         │                  │
└────────────────┘         └───────────────────┘         └──────────────────┘
       │                             │                             │
       └──────────────┬──────────────┴─────────────────────────────┘
                      │
              ┌───────▼───────┐
              │   Services    │   (lógica de negocio)
              └───────┬───────┘
                      │
              ┌───────▼───────┐
              │ Repositories  │   (Spring Data MongoRepository)
              └───────┬───────┘
                      │
              ┌───────▼───────┐
              │   MongoDB     │   colecciones: usuarios, contribuyentes,
              │   (Atlas)     │   obligaciones, documentos, notificaciones,
              └───────────────┘   empleados, ...
```

### Patrón de capas

Cada módulo de negocio respeta la separación:

```
modulo/
├── controller/    ← MVC (Thymeleaf) y/o REST (JSON)
├── service/       ← lógica de negocio, validaciones, transacciones
├── repository/    ← MongoRepository + queries derivadas
├── model/         ← entidades persistibles (@Document)
└── dto/           ← records / objetos de transferencia
```

### Decisiones de arquitectura clave

- **`Document` vive en colección propia (`documentos`)**, no embebida en
  `TaxObligation`. Permite listar todos los documentos de un contribuyente
  en una sola consulta y soporta documentos sueltos (sin obligación).
- **`TaxObligation` referencia documentos por id** (`List<String> documentIds`),
  no por embed. Evita duplicación y mantiene la colección obligaciones liviana.
- **`Role` es un POJO**, no un enum, persistido junto al `User`. Tiene
  `name`, `description` y `permissions`. Constantes canónicas:
  `Role.GERENTE`, `Role.ASESOR`, `Role.CONTRIBUYENTE` (todas con prefijo `ROLE_`).
- **`taxPayerId` en `User`** vincula un usuario CONTRIBUYENTE al
  `TaxPayer` que representa. Es `null` para GERENTE/ASESOR.
- **`update()` de `TaxPayerService` nunca toca colecciones embebidas**
  (obligations, bankAccounts, notifications) — se preservan en cada
  actualización para evitar borrar datos por accidente desde un formulario
  que no las envía.

---

## Estructura del proyecto

```
gestion-tributaria/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/cronos/gestiontributaria/
│   │   │   ├── GestionTributariaApplication.java
│   │   │   ├── auth/                      ← autenticación, usuarios, roles
│   │   │   │   ├── controller/            (AuthController, PasswordController)
│   │   │   │   ├── model/                 (User, Role)
│   │   │   │   ├── repository/            (UserRepository)
│   │   │   │   └── service/               (UserService, CustomUserDetailsService)
│   │   │   │
│   │   │   ├── clientes/                  ← contribuyentes (TaxPayer)
│   │   │   │   ├── controller/
│   │   │   │   │   ├── TaxPayerViewController.java   (MVC /clientes/**)
│   │   │   │   │   ├── TaxPayerRestController.java   (REST /api/contribuyente/**)
│   │   │   │   │   └── PortalViewController.java     (MVC /portal/**)
│   │   │   │   ├── model/                 (TaxPayer, BankAccount)
│   │   │   │   ├── repository/            (TaxPayerRepository)
│   │   │   │   └── service/               (TaxPayerService)
│   │   │   │
│   │   │   ├── obligaciones/              ← obligaciones y documentos
│   │   │   │   ├── controller/            (TaxObligationController, ViewController,
│   │   │   │   │                           MyObligationsViewController)
│   │   │   │   ├── dto/                   (CreateTaxObligationDTO,
│   │   │   │   │                           TaxObligationResponseDTO, …)
│   │   │   │   ├── model/                 (TaxObligation, Document,
│   │   │   │   │                           DocumentRequirement, TaxIndicator,
│   │   │   │   │                           ObligationAssignment)
│   │   │   │   ├── repository/            (TaxObligationRepository, DocumentRepository)
│   │   │   │   └── service/               (TaxObligationService, DocumentService,
│   │   │   │                                TaxObligationDateResolver)
│   │   │   │
│   │   │   ├── empleados/                 ← empleados y tareas
│   │   │   ├── calendarios/               ← reglas de alertas
│   │   │   ├── notification/              ← notificaciones in-app y mail
│   │   │   ├── common/                    ← enums + servicios compartidos
│   │   │   │   ├── (enums: AccountType, EmployeeRole, Priority,
│   │   │   │   │   TaskStatus, TaxObligationStatus, TaxObligationType,
│   │   │   │   │   TaxpayerType, …)
│   │   │   │   ├── service/EmailService.java
│   │   │   │   └── view/                  (PageViewModels, DashboardSummary)
│   │   │   │
│   │   │   └── config/
│   │   │       ├── SecurityConfig.java
│   │   │       ├── DataInitializer.java   (seed inicial — GERENTE + CONTRIBUYENTE demo)
│   │   │       ├── DataRecoveryRunner.java
│   │   │       └── MongoIndexConfig.java  (índices de la colección `documentos`)
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/                    (css/app.css, js/app.js)
│   │       └── templates/
│   │           ├── fragments/layout.html  (layout del panel de administración)
│   │           ├── auth/                  (login, recuperación)
│   │           ├── dashboard.html
│   │           ├── clientes/              (list, form)
│   │           ├── obligaciones/          (list, form, detail)
│   │           ├── empleados/             (list, form)
│   │           ├── calendario/, tareas/, reportes/, configuracion/, admin/
│   │           ├── notificaciones/
│   │           └── portal/                ← portal del contribuyente
│   │               ├── layout.html
│   │               ├── inicio.html
│   │               ├── obligaciones.html
│   │               ├── obligacion-detalle.html
│   │               ├── documentos.html
│   │               └── upload-form.html   (fragmento reutilizable)
│   │
│   └── test/java/com/cronos/gestiontributaria/
│       ├── clientes/
│       │   ├── controller/                (PortalViewControllerTest,
│       │   │                                TaxPayerRestControllerTest)
│       │   ├── model/                     (TaxPayerValidationTest)
│       │   └── service/                   (TaxPayerServiceTest, …B6Test)
│       ├── obligaciones/
│       │   ├── model/                     (DocumentValidationTest)
│       │   └── service/                   (DocumentServiceTest)
│       ├── auth/service/                  (UserServiceB2Test)
│       └── config/                        (MongoIndexConfigTest)
│
└── docs/
    ├── Class Diagram Cronos.png
    └── VENCIMIENTOS 2026 - calendario-tributario-2026.pdf
```

---

## Modelo de datos

### Colecciones MongoDB principales

| Colección         | Entidad             | Identificador        |
|-------------------|---------------------|----------------------|
| `usuarios`        | `User`              | `@Id String id`      |
| `contribuyentes`  | `TaxPayer`          | `@Id String id`      |
| `obligaciones`    | `TaxObligation`     | `@Id String id`      |
| `documentos`      | `Document`          | `@Id String id`      |
| `empleados`       | `Employee`          | `@Id String id`      |
| `notificaciones`  | `Notification`      | `@Id String id`      |

### `User` (colección `usuarios`)

```java
@Document(collection = "usuarios")
class User {
    @Id String id;
    @NotBlank String name;
    @Email @NotBlank String email;
    String passwordHash;          // BCrypt
    @Transient String password;   // solo formulario
    boolean active;
    Role role;                    // POJO embebido
    String taxPayerId;            // ← solo si CONTRIBUYENTE; null para GERENTE/ASESOR
    List<Notification> notifications;
}
```

### `TaxPayer` (colección `contribuyentes`)

```java
@Document(collection = "contribuyentes")
class TaxPayer {
    @Id String id;
    @NotBlank @Size(max=200) String businessName;
    @NotBlank @Size(max=20)  String identificacion;   // NIT o CC
    @NotNull TaxpayerType type;                       // NATURAL_PERSON | LEGAL_ENTITY
    boolean granContribuyente;
    @NotBlank @Email String email;
    @Size(max=20)  String phone;
    @Size(max=300) String address;
    boolean active;
    LocalDate registrationDate;
    List<TaxObligation> obligations;     // embebido (legacy)
    List<BankAccount>   bankAccounts;    // embebido
    List<Notification>  notifications;   // embebido
}
```

### `TaxObligation` (colección `obligaciones`)

```java
@Document(collection = "obligaciones")
class TaxObligation {
    @Id String id;
    TaxObligationType type;          // INCOME_TAX, VAT, WITHHOLDING,
                                     // INDUSTRY_COMMERCE, PATRIMONY
    String taxPayerId;               // referencia
    String fiscalPeriod;             // formato según el tipo
    int taxYear;
    LocalDate dueDate;
    boolean dueDateOverridden;
    String dueDateOverrideReason;
    TaxObligationStatus status;      // PENDING, IN_PROGRESS, COMPLETED,
                                     // OVERDUE, CANCELLED
    String notes;
    List<String> documentIds;        // ← referencia por id, no embebido
    List<Alert> alerts;
    List<TaxIndicator> indicators;
    List<Task> tasks;
}
```

### `Document` (colección `documentos`)

```java
@Document(collection = "documentos")
class Document {
    @Id String id;
    @NotBlank String fileName;            // nombre original
    String storedFileName;                // UUID + extensión en disco
    @NotBlank String fileType;            // MIME
    @NotNull Long fileSize;               // bytes
    LocalDateTime uploadedAt;
    @Size(max=500) String description;
    @NotBlank String taxPayerId;          // dueño
    String obligationId;                  // opcional (puede ser null)
    // campos legacy preservados (name, storagePath, format, sizeBytes)
}
```

### Índices MongoDB (`MongoIndexConfig`)

Sobre la colección `documentos`:

| Índice                                  | Campos                                 | Nota          |
|-----------------------------------------|----------------------------------------|---------------|
| `idx_documentos_taxPayerId`             | `taxPayerId: 1`                        |               |
| `idx_documentos_obligationId`           | `obligationId: 1`                      | `sparse=true` |
| `idx_documentos_taxPayer_fecha`         | `taxPayerId: 1, uploadedAt: -1`        |               |

Creados con `@PostConstruct` al arrancar (idempotente — `createIndex` no
duplica si ya existe con el mismo nombre).

---

## Roles y seguridad

### Roles definidos

| Constante              | Valor (`Role.name`)     | Acceso                                  |
|------------------------|-------------------------|-----------------------------------------|
| `Role.GERENTE`         | `ROLE_GERENTE`          | Panel completo + administración         |
| `Role.ASESOR`          | `ROLE_ASESOR`           | Operación diaria sin admin              |
| `Role.CONTRIBUYENTE`   | `ROLE_CONTRIBUYENTE`    | Solo su propia info via `/portal/**`    |
| `ROLE_USER`            | (default registro)      | Cuenta sin asignar                      |
| `ADMIN`                | (en `SecurityConfig`)   | `/admin/**`                             |

`CustomUserDetailsService` traduce `User.role.name` a una `GrantedAuthority`
de Spring Security usando el prefijo `ROLE_` por convención.

### Reglas de autorización (`SecurityConfig`)

Las reglas se evalúan en orden — **first match wins**:

```java
.requestMatchers("/", "/login", "/403", "/css/**", "/js/**",
                 "/images/**", "/error").permitAll()

// Reglas específicas primero
.requestMatchers(HttpMethod.GET,   "/api/contribuyente")
    .hasAnyRole("GERENTE", "ASESOR")
.requestMatchers(HttpMethod.PATCH, "/api/contribuyente/*/toggle-active")
    .hasRole("GERENTE")
.requestMatchers("/api/contribuyente/**")
    .hasRole("CONTRIBUYENTE")
.requestMatchers("/portal/**")
    .hasRole("CONTRIBUYENTE")
.requestMatchers("/admin/**")
    .hasRole("ADMIN")

// Resto de la API REST (público)
.requestMatchers("/api/**").permitAll()
.anyRequest().authenticated()
```

- CSRF deshabilitado solo para `/api/**`.
- `@EnableMethodSecurity` activo: `@PreAuthorize` se evalúa en cada método.
- Login por formulario en `/login`, logout en `/logout`, sesión basada en
  `JSESSIONID`, redirige a `/dashboard` tras login exitoso.

### Cómo se vincula un User a un TaxPayer

`UserService.vincularContribuyente(email, taxPayerId)`:

```java
public void vincularContribuyente(String email, String taxPayerId) {
    repository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
        user.setRole(Role.contribuyente());
        user.setTaxPayerId(taxPayerId);
        repository.save(user);
    });
}
```

Silencioso si el email no existe (no lanza). Cualquier endpoint del portal
resuelve el `taxPayerId` del usuario autenticado vía
`PortalViewController.resolverTaxPayerId(Authentication)` o
`TaxPayerRestController.resolverTaxPayerId(Authentication)` — **nunca**
desde un parámetro de URL. Si el `taxPayerId` es null o vacío → `403`.

---

## Módulos implementados

### Módulo de Clientes / Contribuyentes (`clientes/`)

**Backend**:
- CRUD completo de `TaxPayer` con validación Jakarta.
- `findByFilters(searchTerm, type, active, pageable)`: búsqueda paginada
  con regex case-insensitive sobre `businessName` + `identificacion`.
  Implementado con `MongoTemplate` para evitar limitaciones de `$expr`
  en `@Query`.
- `update()` preserva las colecciones embebidas (no se borran si el form
  no las envía).
- `toggleActive(id)`: invierte el estado activo del contribuyente.
- Vincula usuarios autenticados (`vincularContribuyente`) al `taxPayerId`.

**Frontend (panel admin)** — `templates/clientes/`:
- `list.html`: tabla con filtros y paginación, acciones de editar/eliminar
  para GERENTE.
- `form.html`: alta y edición con validación visual (Bootstrap-style
  `is-invalid` + `invalid-feedback`).

### Módulo de Obligaciones (`obligaciones/`)

- `TaxObligation`: entidad con fechas calculadas por
  `TaxObligationDateResolver` según calendario tributario 2026 colombiano.
- Validación de formato de `fiscalPeriod` según tipo:
  - `WITHHOLDING`: `^\d{4}-\d{2}$` (mensual)
  - `VAT`: `^\d{4}-B[1-6]$` (bimestral) o `^\d{4}-Q[1-3]$` (cuatrimestral)
  - `INCOME_TAX`, `PATRIMONY`: `^\d{4}$` (anual)
  - `INDUSTRY_COMMERCE`: libre
- Restricción de unicidad: un mismo contribuyente no puede tener dos
  obligaciones con el mismo `(type, fiscalPeriod)`.
- Cambio de estado registrado y notificable por correo.

### Módulo de Documentos

- `DocumentService.uploadDocument(taxPayerId, obligationId, file, description)`:
  - valida tipo MIME contra `cronos.storage.allowed-types`
  - valida tamaño contra `cronos.storage.max-file-size-mb`
  - guarda el archivo físico en `cronos.storage.upload-dir` con nombre
    `UUID + originalFilename`
  - persiste metadatos en `documentos`
  - si `obligationId != null`, añade el id al `documentIds` de esa obligación
- `findByTaxPayer(taxPayerId)` → ordenado por `uploadedAt` desc
- `findByObligation(obligationId)`
- `delete(id)` → borra el archivo físico y el registro

### Portal del Contribuyente (`/portal/**`)

Layout independiente del panel admin (`templates/portal/layout.html`)
con estilos propios. Tres vistas principales + un fragmento reutilizable:

| Ruta                              | Vista                            | Función                              |
|-----------------------------------|----------------------------------|--------------------------------------|
| `GET /portal`                     | redirect → `/portal/inicio`      |                                      |
| `GET /portal/inicio`              | `portal/inicio.html`             | datos de perfil read-only            |
| `GET /portal/obligaciones`        | `portal/obligaciones.html`       | listado de obligaciones              |
| `GET /portal/obligaciones/{id}`   | `portal/obligacion-detalle.html` | detalle + documentos + subida        |
| `GET /portal/documentos`          | `portal/documentos.html`         | todos sus documentos + subida libre  |

**Características clave**:
- Cero formularios de edición de datos personales — solo lectura.
- El único formulario interactivo es la subida de documentos.
- Cada acceso valida que el `taxPayerId` solicitado es el del usuario
  autenticado (guard en el controller).
- Subida vía `fetch()` (`POST /api/contribuyente/me/documentos`), sin
  recarga completa hasta que el upload termina con éxito.

---

## API REST

Todos los endpoints REST viven bajo `/api`. El controlador principal del
módulo de clientes es `TaxPayerRestController` (`/api/contribuyente`).

### Endpoints del contribuyente autenticado

| Método | Ruta                                          | Acceso         | Descripción                                  |
|--------|-----------------------------------------------|----------------|----------------------------------------------|
| GET    | `/api/contribuyente/me`                       | CONTRIBUYENTE  | Datos del contribuyente autenticado          |
| GET    | `/api/contribuyente/me/obligaciones`          | CONTRIBUYENTE  | Sus obligaciones (`TaxObligationResponseDTO`)|
| GET    | `/api/contribuyente/me/documentos`            | CONTRIBUYENTE  | Sus documentos                               |
| POST   | `/api/contribuyente/me/documentos`            | CONTRIBUYENTE  | Subir documento (multipart/form-data)        |

**`POST /me/documentos`** acepta:
- `file` (RequestPart, requerido)
- `obligationId` (RequestParam, opcional — si viene debe pertenecer al usuario)
- `description` (RequestParam, opcional)

Respuestas:
- `201 Created` con el `Document` persistido
- `400 Bad Request` si tipo o tamaño no válido
- `403 Forbidden` si el `obligationId` no pertenece al usuario

### Endpoints administrativos (sobre el mismo controlador)

| Método | Ruta                                          | Acceso          | Descripción                              |
|--------|-----------------------------------------------|-----------------|------------------------------------------|
| GET    | `/api/contribuyente`                          | GERENTE, ASESOR | Listado paginado con filtros             |
| PATCH  | `/api/contribuyente/{id}/toggle-active`       | GERENTE         | Activar/desactivar contribuyente         |

**`GET /api/contribuyente`** acepta query params:
- `q` (texto libre, busca en businessName e identificacion)
- `tipo` (`NATURAL_PERSON` | `LEGAL_ENTITY`)
- `activo` (`true` | `false`)
- `page` (default 0), `size` (default 20)

Respuesta: `Page<TaxPayer>` serializado como `{content, totalElements, totalPages, number, ...}`.

### Otros endpoints REST

- `/api/obligaciones` — gestión de obligaciones (`TaxObligationController`).
- `/api/auth/**` — login programático y recuperación de password.

---

## Vistas (Thymeleaf)

### Panel de administración

Usa el fragmento `fragments/layout.html` con el shell:
```html
th:replace="~{fragments/layout :: shell(pageTitle, pageSubtitle, activeSection, ~{::content})}"
```

Páginas: `dashboard`, `clientes/{list,form}`, `obligaciones/{list,form,detail}`,
`empleados/{list,form}`, `calendario/index`, `tareas/index`, `reportes/index`,
`configuracion/index`, `admin/panel`, `notificaciones/list`.

### Portal del contribuyente

Layout propio (`portal/layout.html`) con `th:fragment="layout(title, content)"`.
Las vistas se invocan así:
```html
th:replace="~{portal/layout :: layout('Mi título', ~{::content})}"
```

El fragmento `portal/upload-form.html` se reutiliza en `obligacion-detalle.html`
y `documentos.html` pasando el `obligationId` o `null` según corresponda.

---

## Configuración

### `application.properties`

```properties
spring.application.name=cronos

# Importa variables desde .env (opcional)
spring.config.import=optional:file:./.env[.properties]

# MongoDB Atlas
spring.mongodb.uri=mongodb+srv://${MONGO_ATLAS_USER_NAME}:${MONGO_ATLAS_PASSWORD}@cronos-cluster.ngv13rn.mongodb.net/cronos?retryWrites=true&w=majority&appName=cronos-cluster
spring.mongodb.database=cronos
spring.data.mongodb.auto-index-creation=false

# Thymeleaf
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.cache=false

# SMTP (Gmail por defecto)
app.mail.service=${EMAIL_SERVICE:gmail}
app.mail.from=${EMAIL_FROM:${EMAIL_USER:}}
spring.mail.host=${EMAIL_HOST:smtp.gmail.com}
spring.mail.port=${EMAIL_PORT:587}
spring.mail.username=${EMAIL_USER:}
spring.mail.password=${EMAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Almacenamiento de documentos
cronos.storage.upload-dir=uploads/documentos
cronos.storage.max-file-size-mb=10
cronos.storage.allowed-types=application/pdf,image/jpeg,image/png,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

# Multipart de Spring MVC (debe coincidir con max-file-size-mb)
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=11MB
```

### Variables de entorno (`.env` en la raíz)

```bash
# MongoDB Atlas
MONGO_ATLAS_USER_NAME=tu_usuario
MONGO_ATLAS_PASSWORD=tu_password

# Email
EMAIL_USER=correo@gmail.com
EMAIL_PASSWORD=app_password_de_gmail
EMAIL_FROM=correo@gmail.com
EMAIL_SERVICE=gmail
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
```

---

## Cómo levantar el proyecto

### Requisitos

- JDK 17+
- Maven 3.6+ (no es necesario instalarlo; el proyecto trae `mvnw`)
- Acceso a MongoDB Atlas (o un Mongo local, ajustando `spring.mongodb.uri`)
- Cuenta SMTP para envío de correos (opcional para desarrollo)

### Pasos

```bash
# 1. Configura .env con las variables descritas arriba.

# 2. Compila el proyecto
./mvnw clean compile

# 3. Levanta la aplicación
./mvnw spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

Al arrancar por primera vez, `DataInitializer` crea dos usuarios si no
existen (ver [Credenciales por defecto](#credenciales-por-defecto)).

---

## Tests

El proyecto tiene cobertura amplia sobre los módulos críticos.

### Suites por categoría

| Suite                            | Tests | Cubre                                              |
|----------------------------------|-------|----------------------------------------------------|
| `TaxPayerValidationTest`         | 4     | Bean Validation sobre `TaxPayer`                   |
| `DocumentValidationTest`         | 4     | Bean Validation sobre `Document`                   |
| `MongoIndexConfigTest`           | 4     | Creación de los 3 índices de `documentos`          |
| `TaxPayerServiceTest`            | 5     | `update()` preserva colecciones embebidas (B-1)    |
| `TaxPayerServiceB6Test`          | 5     | Paginación y filtros (`findByFilters`)             |
| `UserServiceB2Test`              | 4     | Vinculación User ↔ TaxPayer                        |
| `DocumentServiceTest`            | 5     | Subida, validación de tipo/tamaño, asociación      |
| `TaxPayerRestControllerTest`     | 18    | Todos los endpoints REST + reglas de acceso        |
| `PortalViewControllerTest`       | 15    | Vistas MVC del portal + guards de pertenencia      |
| **Total**                        | **64**|                                                    |

### Cómo correr los tests

```bash
# Toda la suite (recomendado tras añadir muchos archivos nuevos)
./mvnw clean test

# Una clase específica
./mvnw test -Dtest=PortalViewControllerTest

# Varias clases
./mvnw test -Dtest="TaxPayerValidationTest,DocumentServiceTest"
```

### Notas técnicas sobre los tests

- **Spring Boot 4 movió `@WebMvcTest`** de
  `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` a
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- **`@MockBean` está deprecado** → se usa `@MockitoBean` de
  `org.springframework.test.context.bean.override.mockito`.
- **`@WebMvcTest` no carga `SecurityConfig` real** (arrastra
  `CustomUserDetailsService` y `UserRepository`). En su lugar, los tests
  importan una `MethodSecurityTestConfig` interna con `@EnableMethodSecurity`
  para que `@PreAuthorize` se aplique en el contexto de test.
- **CSRF en tests POST/PATCH**: como el `SecurityConfig` excluye `/api/**`
  pero los tests no cargan ese config, las peticiones POST/PATCH añaden
  `.with(csrf())` (`SecurityMockMvcRequestPostProcessors.csrf`).
- **No hay Mongo embebido en el classpath**. `MongoIndexConfigTest` usa
  Mockito sobre `MongoTemplate`/`MongoCollection` con `ArgumentCaptor<IndexOptions>`
  para validar nombres y opciones de los índices sin levantar Mongo real.

---

## Credenciales por defecto

`DataInitializer` crea dos usuarios al arrancar (si no existen):

| Rol            | Email                       | Contraseña   | `taxPayerId`        |
|----------------|-----------------------------|--------------|---------------------|
| GERENTE        | `gerente@cronos.com`        | `admin123`   | (no aplica)         |
| CONTRIBUYENTE  | `contribuyente@cronos.com`  | `test1234`   | `test-taxpayer-id`  |

> El `taxPayerId` del CONTRIBUYENTE es un placeholder. Para probar el portal
> de extremo a extremo hay que crear primero un `TaxPayer` real con
> `_id="test-taxpayer-id"` (desde el panel de admin como GERENTE o
> directamente en Mongo) o esperar a que el flujo de registro de
> contribuyente (B-3) vincule automáticamente al User.

---

## Limitaciones y trabajo pendiente

- **Documentos legacy**: el modelo `Document` conserva campos antiguos
  (`name`, `storagePath`, `format`, `sizeBytes`) para no romper código
  existente. Eventualmente migrar a los nuevos (`fileName`, `storedFileName`,
  `fileType`, `fileSize`).
- **Migración de documentos embebidos**: si en producción hay
  `TaxObligation.documents` embebidos, se necesita un script que los
  mueva a la colección `documentos` y rellene `documentIds`.
- **Descarga de documentos** (`/portal/documentos/{id}/descargar`):
  declarada en las vistas, pendiente de implementar el endpoint que
  sirva el archivo desde disco con headers apropiados.
- **`Page<TaxPayer>` se serializa directamente**: Spring imprime un warning
  recomendando `PagedModel`/`VIA_DTO`. Si la API se va a estabilizar como
  contrato público, activar `@EnableSpringDataWebSupport(pageSerializationMode=VIA_DTO)`.
- **Almacenamiento local**: actualmente los archivos viven en
  `uploads/documentos/` del servidor. Para producción habría que migrar a
  S3 o GridFS.

---

## Licencia y autoría

Proyecto académico — Universidad de Santander (UDES) — Semestre 7,
asignatura *Programación Web*. Mini proyecto Cronos, equipo de gestión
tributaria.
