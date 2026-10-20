# design-service

Diseños: versiones del arte y aprobación del cliente.

## Funcionalidad

- `GET|POST|PUT|DELETE /api/v1/disenos` (escritura ADMIN; el cliente ve los de sus pedidos).
- Cada nueva versión reemplaza a la anterior; no se sube otra si ya hay una aprobada.
- `POST /api/v1/disenos/{id}/decision` → el cliente APRUEBA u OBSERVA (con comentario) la versión vigente.
- Valida el pedido en sales-service con Feign.

## Pruebas

- `DisenoRepositoryTest`, `DisenoFlujoIntegrationTest`

## Ejecución

Puerto `8096`. Base local `sigip_design` (MySQL); en la nube, esquema propio en PostgreSQL.

Variables: `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (o `DB_URL` completa).

```bash
mvn -pl backend/design-service -am spring-boot:run
```
