package cl.vidasalud.appointments.messaging.kafka;

import cl.vidasalud.appointments.dto.AtencionEvent;
import cl.vidasalud.appointments.dto.AtencionKafkaEvent;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor Kafka: publica cada cambio del ciclo de vida de una atención en vidasalud.atenciones.eventos.
 * La clave es el id de la atención, así sus eventos caen en la misma partición y mantienen el orden.
 * Un fallo de Kafka se registra pero nunca interrumpe el flujo principal (RabbitMQ + base de datos).
 */
@Component
public class AtencionKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(AtencionKafkaProducer.class);

    private final KafkaTemplate<String, AtencionKafkaEvent> template;
    private final Clock clock;
    private final String topic;

    public AtencionKafkaProducer(KafkaTemplate<String, AtencionKafkaEvent> template, Clock clock,
                                 @Value("${app.kafka.topic.atenciones-eventos}") String topic) {
        this.template = template;
        this.clock = clock;
        this.topic = topic;
    }

    public void publicar(AtencionEvent evento) {
        if (evento == null || evento.atencionId() == null) {
            log.warn("Evento sin id de atención: no se publica en Kafka");
            return;
        }
        AtencionKafkaEvent kafkaEvent = AtencionKafkaEvent.desde(evento, clock.instant());
        try {
            template.send(topic, kafkaEvent.atencionId().toString(), kafkaEvent).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.warn("Kafka: no se pudo publicar el evento {} de la atención {}: {}",
                            kafkaEvent.tipo(), kafkaEvent.atencionId(), error.toString());
                } else if (resultado != null && resultado.getRecordMetadata() != null) {
                    log.info("Kafka ← {} atención={} partición={} offset={}", kafkaEvent.tipo(),
                            kafkaEvent.atencionId(), resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset());
                }
            });
        } catch (RuntimeException e) {
            log.warn("Kafka no disponible; el evento {} de la atención {} solo viaja por RabbitMQ: {}",
                    kafkaEvent.tipo(), kafkaEvent.atencionId(), e.toString());
        }
    }
}
