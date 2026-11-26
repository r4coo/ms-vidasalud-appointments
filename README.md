# ms-vidasalud-appointments

Microservicio principal: atenciones (SOLICITADA→CONFIRMADA→EN_ESPERA→EN_ATENCION→CERRADA/CANCELADA), reglas de negocio, auditoría. Publica comandos/eventos a RabbitMQ y el historial de eventos a Apache Kafka.

- Puerto: **8082**
- Stack: Spring Boot 4, Java 25, Gradle.
- Base de datos: PostgreSQL (db_appointments) con Flyway.
- RabbitMQ: publica `atencion.confirmar` y eventos; consume `q.auditoria` (ACK manual, DLQ).
- Kafka (productor): publica cada cambio de estado y el resultado del catálogo (`CONFIRMADA`/`RECHAZADA`) en `vidasalud.atenciones.eventos`, con clave = id de la atención (orden por atención dentro de su partición), JSON propio, `acks=all` e idempotencia.
- Si Kafka no responde, se registra un aviso (`max.block.ms=3000`) y el flujo con RabbitMQ continúa.

Variables Kafka: `KAFKA_BOOTSTRAP_SERVERS` (por defecto `localhost:9092`), `app.kafka.topic.atenciones-eventos`.

## Ejecutar
```bash
./gradlew test
./gradlew bootRun     # requiere RabbitMQ/PostgreSQL (ver repo de infraestructura: vidasalud-infra)
docker build -t ms-vidasalud-appointments .
```
Variables de entorno: ver `src/main/resources/application.properties` y el `compose.yaml` del repo de infraestructura.
No subir `.env` ni credenciales.
