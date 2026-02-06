# Catalog Service

Catalog microservice extracted from OpenCart for products, categories, and manufacturers.

## Stack
- Spring Boot 3
- PostgreSQL
- Kafka events
- Keycloak (OAuth2 Resource Server)
- OpenShift-ready manifests

## Local run (dev)
Set env vars then run:

```
CATALOG_DB_URL=jdbc:postgresql://localhost:5432/catalog \
CATALOG_DB_USER=catalog \
CATALOG_DB_PASSWORD=catalog \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/catalog \
mvn spring-boot:run
```

## Roles
- `catalog_read` for read-only
- `catalog_admin` for write access

## API
- `GET /api/catalog/products`
- `GET /api/catalog/products/{id}`
- `POST /api/catalog/products`
- `PUT /api/catalog/products/{id}`
- `DELETE /api/catalog/products/{id}`

- `GET /api/catalog/categories`
- `GET /api/catalog/categories/{id}`
- `POST /api/catalog/categories`
- `PUT /api/catalog/categories/{id}`
- `DELETE /api/catalog/categories/{id}`

- `GET /api/catalog/manufacturers`
- `GET /api/catalog/manufacturers/{id}`
- `POST /api/catalog/manufacturers`
- `PUT /api/catalog/manufacturers/{id}`
- `DELETE /api/catalog/manufacturers/{id}`

## Events
Kafka topic: `catalog.events` (configurable via `CATALOG_EVENTS_TOPIC`).

Event types:
- `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_DELETED`
- `CATEGORY_CREATED`, `CATEGORY_UPDATED`, `CATEGORY_DELETED`
- `MANUFACTURER_CREATED`, `MANUFACTURER_UPDATED`, `MANUFACTURER_DELETED`

## OpenShift
See `deploy/openshift/catalog-service.yaml` for a baseline Deployment, Service and Route.
