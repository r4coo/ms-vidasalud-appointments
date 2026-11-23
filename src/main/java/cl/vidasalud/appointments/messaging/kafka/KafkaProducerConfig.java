package cl.vidasalud.appointments.messaging.kafka;

import cl.vidasalud.appointments.dto.AtencionKafkaEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/** Cliente productor Kafka: brokers del clúster, serializers y garantías de entrega. */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, AtencionKafkaEvent> atencionProducerFactory(
            @Value("${app.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        cfg.put(ProducerConfig.ACKS_CONFIG, "all");                    // el líder espera a las réplicas en sincronía
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);       // los reintentos no duplican mensajes
        cfg.put(ProducerConfig.RETRIES_CONFIG, 3);
        cfg.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        cfg.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        cfg.put(ProducerConfig.LINGER_MS_CONFIG, 5);                   // agrupa envíos cercanos en un lote
        cfg.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        cfg.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);             // si Kafka cae, no bloquea la petición HTTP
        return new DefaultKafkaProducerFactory<>(cfg, new StringSerializer(), new EventoJsonSerializer<>());
    }

    @Bean
    public KafkaTemplate<String, AtencionKafkaEvent> atencionKafkaTemplate(
            ProducerFactory<String, AtencionKafkaEvent> atencionProducerFactory) {
        return new KafkaTemplate<>(atencionProducerFactory);
    }
}
