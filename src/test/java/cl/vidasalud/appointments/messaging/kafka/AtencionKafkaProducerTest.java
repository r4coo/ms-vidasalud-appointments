package cl.vidasalud.appointments.messaging.kafka;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.vidasalud.appointments.dto.AtencionEvent;
import cl.vidasalud.appointments.dto.AtencionKafkaEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class AtencionKafkaProducerTest {
    private static final String TOPIC = "vidasalud.atenciones.eventos";

    private KafkaTemplate<String, AtencionKafkaEvent> template;
    private AtencionKafkaProducer producer;
    private final UUID id = UUID.randomUUID();
    private final AtencionEvent evento = new AtencionEvent(id, AtencionEvent.ESTADO, "SOLICITADA", null,
            "paciente@vidasalud.cl", "Medicina general", LocalDate.of(2026, 11, 3), LocalTime.of(10, 0));

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        template = mock(KafkaTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC);
        producer = new AtencionKafkaProducer(template, clock, TOPIC);
    }

    @Test
    void publica_conLaClaveDeLaAtencionYElDtoCompleto() {
        when(template.send(anyString(), anyString(), any(AtencionKafkaEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publicar(evento);

        ArgumentCaptor<AtencionKafkaEvent> captor = ArgumentCaptor.forClass(AtencionKafkaEvent.class);
        verify(template).send(eq(TOPIC), eq(id.toString()), captor.capture());
        AtencionKafkaEvent enviado = captor.getValue();
        assertEquals(id, enviado.atencionId());
        assertEquals("SOLICITADA", enviado.estado());
        assertEquals("paciente@vidasalud.cl", enviado.paciente());
        assertEquals(AtencionKafkaEvent.VERSION, enviado.version());
        assertEquals(Instant.parse("2026-11-02T12:00:00Z"), enviado.ocurridoEn());
        assertNotNull(enviado.eventId());
    }

    @Test
    void siKafkaFalla_elFlujoPrincipalNoSeInterrumpe() {
        when(template.send(anyString(), anyString(), any(AtencionKafkaEvent.class)))
                .thenThrow(new IllegalStateException("broker caído"));
        assertDoesNotThrow(() -> producer.publicar(evento));
    }

    @Test
    void siElEnvioFallaDespues_soloSeRegistra() {
        when(template.send(anyString(), anyString(), any(AtencionKafkaEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("timeout")));
        assertDoesNotThrow(() -> producer.publicar(evento));
    }

    @Test
    void eventoSinIdDeAtencion_noSePublica() {
        producer.publicar(new AtencionEvent(null, AtencionEvent.ESTADO, "SOLICITADA", null, "a@b.cl",
                "Medicina general", LocalDate.of(2026, 11, 3), LocalTime.of(10, 0)));
        verify(template, never()).send(anyString(), anyString(), any(AtencionKafkaEvent.class));
    }
}
