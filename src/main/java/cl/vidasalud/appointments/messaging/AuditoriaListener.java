package cl.vidasalud.appointments.messaging;

import cl.vidasalud.appointments.dto.AtencionEvent;
import cl.vidasalud.appointments.service.AuditoriaService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/** Dominio auditoría: registra todo evento del topic y actualiza el estado de la atención. */
@Component
public class AuditoriaListener {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaListener.class);

    private final AuditoriaService service;
    private final JsonCodec json;
    private final AckSupport ack;

    public AuditoriaListener(AuditoriaService service, JsonCodec json, AckSupport ack) {
        this.service = service;
        this.json = json;
        this.ack = ack;
    }

    @RabbitListener(queues = RabbitNames.Q_AUDITORIA)
    public void onEvento(Message message, Channel channel) throws IOException {
        try {
            AtencionEvent event = json.fromBytes(message.getBody(), AtencionEvent.class);
            if (event.atencionId() == null || event.tipo() == null) {
                throw new IllegalArgumentException("Evento incompleto");
            }
            service.registrar(message.getMessageProperties().getReceivedRoutingKey(),
                    new String(message.getBody(), StandardCharsets.UTF_8), event);
            ack.ack(channel, message);
            log.info("Auditoría registrada: atencion={} tipo={}", event.atencionId(), event.tipo());
        } catch (IllegalArgumentException e) {            // no recuperable
            ack.rejectToDlq(channel, message, e.getMessage());
        } catch (DataAccessException e) {                 // recuperable
            ack.retryOrDlq(channel, message, RabbitNames.Q_AUDITORIA, e);
        } catch (RuntimeException e) {
            ack.rejectToDlq(channel, message, "error inesperado: " + e);
        }
    }
}
