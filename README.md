# DeepBlue Rescue

Proyecto de persistencia para una plataforma de rescate y rehabilitacion de fauna marina. El objetivo del laboratorio es trabajar el recorrido completo entre el modelo relacional, migraciones Flyway, entidades JPA, repositories y pruebas de integracion contra PostgreSQL real.

## Tecnologias

- Java 21
- Spring Boot 4.1.x
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Testcontainers
- Maven

## Modelo de datos

```mermaid
erDiagram
    RESCUE_CENTER ||--o{ RESCUE_CASE : manages
    RESCUE_CASE ||--|| ANIMAL : involves
    ANIMAL ||--|| MEDICAL_RECORD : has
    SPECIALIST }o--o{ EXPERTISE : possesses
    ANIMAL ||--o{ TREATMENT : receives
    SPECIALIST ||--o{ TREATMENT : performs
```

Tablas principales:

- `rescue_centers`
- `rescue_cases`
- `animals`
- `medical_records`
- `specialists`
- `expertise`
- `treatments`

Tabla asociativa:

- `specialist_expertise`

## Relaciones

- `RescueCenter 1:N RescueCase`
- `RescueCase 1:1 Animal`
- `Animal 1:1 MedicalRecord`
- `Specialist N:M Expertise`
- `Animal 1:N Treatment`
- `Specialist 1:N Treatment`

Las relaciones 1:1 se refuerzan en PostgreSQL con una restriccion `UNIQUE` sobre la clave foranea correspondiente.

## Flyway

Flyway es el responsable de crear y evolucionar el esquema. Hibernate esta configurado con:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Por eso Hibernate valida el esquema, pero no crea ni actualiza las tablas.

Migraciones:

- `V1__create_schema.sql`: crea tablas, constraints e indices.
- `V2__insert_expertise_catalog.sql`: carga el catalogo inicial de especialidades.
- `V3__add_tracking_device_to_animal.sql`: agrega `tracking_device_code` a `animals`.

## Query Methods

Entre los Query Methods implementados estan:

- `RescueCenterRepository.findByCode(...)`
- `RescueCaseRepository.findByCaseCode(...)`
- `RescueCaseRepository.findByStatusOrderByRescueDateAsc(...)`
- `RescueCaseRepository.findByRescueCenterCode(...)`
- `RescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc(...)`
- `AnimalRepository.findByAnimalCode(...)`
- `AnimalRepository.findByCommonNameContainingIgnoreCase(...)`
- `AnimalRepository.findByRescueCaseStatus(...)`
- `AnimalRepository.findByRescueCaseRescueCenterCode(...)`
- `ExpertiseRepository.findByNameIgnoreCase(...)`
- `TreatmentRepository.findByAnimalIdOrderByPerformedAtAsc(...)`

## Consultas JPQL

Se implementaron consultas JPQL para:

- especialistas activos segun expertise;
- tratamientos entre dos fechas;
- tratamientos realizados a animales de un centro;
- tratamientos hechos por especialistas con determinada expertise;
- animales en un estado concreto que hayan recibido tratamiento de un especialista con determinada expertise.

## Ejecutar la aplicacion

Para ejecutar la aplicacion se necesita PostgreSQL disponible. Por defecto se intenta conectar a:

```text
jdbc:postgresql://localhost:5432/deepblue
```

Usuario y clave por defecto:

```text
postgres / postgres
```

Tambien se pueden usar las variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD`.

En Windows:

```bash
mvnw.cmd spring-boot:run
```

En Linux/macOS:

```bash
./mvnw spring-boot:run
```

## Ejecutar los tests

Los tests usan Testcontainers, por lo que Docker Desktop debe estar iniciado.

Windows:

```bash
mvnw.cmd clean test
```

Linux/macOS:

```bash
./mvnw clean test
```

Durante la ejecucion Testcontainers levanta temporalmente PostgreSQL 18 Alpine con la base `deepblue_test`. `@ServiceConnection` entrega la conexion al contexto de Spring Boot y, al finalizar las pruebas, el contenedor se elimina.

En otra terminal se puede ejecutar:

```bash
docker ps
```

para observar el contenedor mientras los tests estan corriendo.

## Pruebas incluidas

`PersistenceIntegrationIT` comprueba, entre otras cosas:

- ejecucion de migraciones Flyway;
- metodos heredados de `JpaRepository`;
- relaciones 1:N, 1:1 y N:M;
- Query Methods simples y con navegacion de asociaciones;
- consultas JPQL;
- orden cronologico de tratamientos;
- busqueda por intervalo de fechas;
- constraints `UNIQUE`, `FOREIGN KEY` y `CHECK` de PostgreSQL;
- escenario integrador de DeepBlue Caribbean;
- reto final de animales en rehabilitacion tratados por especialistas con experiencia en Trauma.

## API REST

| Operacion | Endpoint | Respuesta |
|---|---|---|
| Casos por estado | `GET /api/rescue-cases?status=ADMITTED` | `200` lista |
| Caso por codigo | `GET /api/rescue-cases/{caseCode}` | `200` / `404` |
| Cambiar estado | `PATCH /api/rescue-cases/{caseCode}/status` body `{ "status": "..." }` | `200` / `400` / `404` / `409` |
| Animal | `GET /api/animals/{animalCode}` | `200` / `404` |
| Tratamientos del animal | `GET /api/animals/{animalCode}/treatments` | `200` lista |
| Elegibilidad | `GET /api/animals/{animalCode}/treatment-eligibility` | `200` `{ animalCode, eligible }` / `404` |
| Registrar tratamiento | `POST /api/treatments` | `201` / `400` / `404` / `409` |

Todos los errores usan el mismo formato (`ErrorResponse`), generado por `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-10-22T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "details": { "description": "Description is required" }
}
```

- `400`: validacion de Bean Validation (`@Valid`), JSON invalido o parametro con valor invalido.
- `404`: `ResourceNotFoundException`.
- `409`: `BusinessRuleException` (transicion de estado invalida, especialista inactivo, caso liberado/cerrado...).

### Datos de ejemplo (perfil `dev`)

Con el perfil `dev`, `DemoDataLoader` inserta (solo si no existen) un centro `CTR-CARIBE`, seis casos `CASE-001`..`CASE-006` (uno o mas por estado) con sus animales `AN-001`..`AN-006`, los especialistas `SP-001`, `SP-002` (activos) y `SP-003` (inactivo) y algunos tratamientos.

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

## Frontend React (`deepblue-ui/`)

Interfaz construida con React + Vite + TanStack Query v5 que consume el API anterior.

```text
deepblue-ui/src/
├── api/         client.js (request + ApiError), rescueCases.js, animals.js, treatments.js
├── hooks/       useRescueCases.js, useAnimal.js, useRegisterTreatment.js
├── components/  ErrorMessage, CaseList, CaseDetail, ChangeStatus, TreatmentList, TreatmentForm
├── App.jsx
├── index.css
└── main.jsx     QueryClientProvider
```

Vite reenvia `/api` a `http://localhost:8080` (ver `vite.config.js`), por lo que no hace falta configurar CORS.

Con el backend ya levantado:

```bash
cd deepblue-ui
npm install
npm run dev
```

y abrir `http://localhost:5173`.
