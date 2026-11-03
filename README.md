# ms-vidasalud-appointments

Microservicio principal: atenciones (SOLICITADA→CONFIRMADA→EN_ESPERA→EN_ATENCION→CERRADA/CANCELADA), reglas de negocio, auditoría. Publica comandos/eventos a RabbitMQ.

- Puerto: **8082**
- Stack: Spring Boot 4, Java 25, Gradle.
- Base de datos: PostgreSQL (db_appointments) con Flyway.
- RabbitMQ: publica `atencion.confirmar` y eventos; consume `q.auditoria` (ACK manual, DLQ).

## Ejecutar
```bash
./gradlew test
./gradlew bootRun     # requiere RabbitMQ/PostgreSQL (ver repo de infraestructura: vidasalud-infra)
docker build -t ms-vidasalud-appointments .
```
Variables de entorno: ver `src/main/resources/application.properties` y el `compose.yaml` del repo de infraestructura.
No subir `.env` ni credenciales.
