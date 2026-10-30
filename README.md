# property-maintenance-platform
Cloud-native property maintenance platform built with Java, Spring Boot, AWS, Kubernetes, and generative AI.

## Property API

- `POST /api/properties` creates a property and returns `201 Created`, the created property,
  and an absolute `Location` header.
- `GET /api/properties` returns all properties.
- `GET /api/properties/{id}` returns a property, or `404 Not Found` if absent.
- Errors are returned as RFC 9457 problem details (`application/problem+json`);
  validation failures list each invalid field under `errors`.

Create requests contain `name`, `addressLine1`, `city`, `state`, and `zipCode`.
All fields are required, must contain non-whitespace text, and have a maximum length
of 255 characters. Invalid requests return `400 Bad Request`.

```json
{
  "name": "Example Property",
  "addressLine1": "100 Example Ave",
  "city": "Sampletown",
  "state": "GA",
  "zipCode": "30000"
}
```

## Unit API

- `POST /api/properties/{propertyId}/units` creates a unit under an existing property.
  Returns `201 Created`, `{ "id": 7, "unitNumber": "Demo-101", "propertyId": 42 }`,
  and an absolute `Location` header pointing to `/api/units/{id}`.
- `GET /api/properties/{propertyId}/units` lists units for an existing property.
  An existing property without units returns `[]`; a missing property returns `404`.
- `GET /api/units/{id}` returns a unit, or `404` if absent.

Create requests contain only `unitNumber`, which is required, non-blank, and at most
50 characters. Missing parent properties return `404`; errors use the same problem
details format as the Property API. All examples below use fictional data.

```json
{ "unitNumber": "Demo-101" }
```

## Local development

Requires Java 25 and Docker. Set `DB_PASSWORD` in `.env` (gitignored, read by Docker Compose)
and start PostgreSQL:

```sh
docker compose up -d
```

The application reads its database settings from environment variables. Spring Boot does
not load `.env`, so export them in the shell that runs the application or tests:

| Variable      | Required | Default                                               |
|---------------|----------|-------------------------------------------------------|
| `DB_PASSWORD` | yes      | none                                                  |
| `DB_URL`      | no       | `jdbc:postgresql://localhost:5432/property_maintenance` |
| `DB_USERNAME` | no       | `property_app`                                        |

The `local` profile lets Hibernate create and update the schema (`ddl-auto: update`).
The default configuration does not manage the schema, and no migration tool is used yet.

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Tests

- `./mvnw -Dtest=PropertyControllerTests test` runs the HTTP, validation and error-response
  tests with the service mocked. These need no database.
- `./mvnw verify` runs all tests. `PropertyApiIntegrationTests` and the context test need
  the local PostgreSQL database and `DB_PASSWORD`. Integration tests use the `local` profile
  and roll back their data after each test.

Use `mvnw.cmd` on Windows.

Unit tests follow the same split:

- `./mvnw -Dtest=UnitControllerTests test` runs database-free controller tests.
- `./mvnw -Dtest=UnitApiIntegrationTests test` runs the Unit API integration tests
  against local PostgreSQL using the `local` profile, with test data rolled back.
