package cl.vidasalud.appointments.messaging.kafka;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.vidasalud.appointments.dto.AtencionEvent;
import cl.vidasalud.appointments.dto.AtencionKafkaEvent;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventoJsonSerializerTest {
    private final EventoJsonSerializer<AtencionKafkaEvent> serializer = new EventoJsonSerializer<>();

    @Test
    void serializaElDtoComoJson() {
        UUID id = UUID.randomUUID();
        AtencionKafkaEvent evento = AtencionKafkaEvent.desde(new AtencionEvent(id, AtencionEvent.CONFIRMADA,
                "CONFIRMADA", null, "paciente@vidasalud.cl", "Kinesiología", LocalDate.of(2026, 11, 3),
                LocalTime.of(10, 30)), Instant.parse("2026-11-02T12:00:00Z"));

        String json = new String(serializer.serialize("vidasalud.atenciones.eventos", evento), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"atencionId\":\"" + id + "\""), json);
        assertTrue(json.contains("\"tipo\":\"CONFIRMADA\""), json);
        assertTrue(json.contains("\"paciente\":\"paciente@vidasalud.cl\""), json);
        assertTrue(json.contains("2026-11-03"), json);
    }

    @Test
    void valorNulo_seSerializaComoNulo() {
        assertNull(serializer.serialize("vidasalud.atenciones.eventos", null));
    }
}
