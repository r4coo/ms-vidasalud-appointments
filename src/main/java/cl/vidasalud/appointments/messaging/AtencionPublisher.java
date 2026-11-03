package cl.vidasalud.appointments.messaging;

import cl.vidasalud.appointments.dto.AtencionEvent;
import cl.vidasalud.appointments.dto.ConfirmarAtencionCommand;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Productor: comandos al exchange direct y eventos de estado al exchange topic. */
@Component
public class AtencionPublisher {
    private final RabbitTemplate template;
    private final JsonCodec json;

    public AtencionPublisher(RabbitTemplate template, JsonCodec json) {
        this.template = template;
        this.json = json;
    }

    public void publicarComando(ConfirmarAtencionCommand command) {
        template.send(RabbitNames.EX_ATENCIONES, RabbitNames.RK_ATENCION_CONFIRMAR, build(command));
    }

    public void publicarEstado(AtencionEvent event) {
        template.send(RabbitNames.EX_EVENTOS, RabbitNames.RK_ATENCION_ESTADO, build(event));
    }

    private Message build(Object payload) {
        return MessageBuilder.withBody(json.toBytes(payload))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(UUID.randomUUID().toString())
                .build();
    }
}
