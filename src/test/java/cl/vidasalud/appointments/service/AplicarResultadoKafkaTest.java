package cl.vidasalud.appointments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.vidasalud.appointments.dto.AtencionEvent;
import cl.vidasalud.appointments.messaging.AtencionPublisher;
import cl.vidasalud.appointments.model.Atencion;
import cl.vidasalud.appointments.model.EstadoAtencion;
import cl.vidasalud.appointments.repository.AtencionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** El resultado del catálogo (RabbitMQ) también se publica en Kafka para el análisis en streaming. */
class AplicarResultadoKafkaTest {
    private AtencionRepository repo;
    private AtencionPublisher publisher;
    private AtencionService service;

    @BeforeEach
    void setUp() {
        repo = mock(AtencionRepository.class);
        publisher = mock(AtencionPublisher.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneId.of("America/Santiago"));
        service = new AtencionService(repo, publisher, clock);
    }

    private Atencion solicitada() {
        Atencion a = new Atencion(UUID.randomUUID(), "paciente@vidasalud.cl", 1L, "Medicina general",
                LocalDate.of(2026, 11, 3), LocalTime.of(10, 0));
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void confirmada_cambiaElEstadoYPublicaEnKafka() {
        Atencion a = solicitada();
        service.aplicarResultado(a.getId(), AtencionEvent.CONFIRMADA, null);

        assertEquals(EstadoAtencion.CONFIRMADA, a.getEstado());
        ArgumentCaptor<AtencionEvent> captor = ArgumentCaptor.forClass(AtencionEvent.class);
        verify(publisher).publicarEventoKafka(captor.capture());
        assertEquals(AtencionEvent.CONFIRMADA, captor.getValue().tipo());
        assertEquals("CONFIRMADA", captor.getValue().estado());
        verify(publisher, never()).publicarEstado(any());   // el evento de RabbitMQ no se duplica
    }

    @Test
    void rechazada_publicaElMotivoEnKafka() {
        Atencion a = solicitada();
        service.aplicarResultado(a.getId(), AtencionEvent.RECHAZADA, "Sin cupos disponibles en ese horario");

        ArgumentCaptor<AtencionEvent> captor = ArgumentCaptor.forClass(AtencionEvent.class);
        verify(publisher).publicarEventoKafka(captor.capture());
        assertEquals(AtencionEvent.RECHAZADA, captor.getValue().tipo());
        assertEquals("Sin cupos disponibles en ese horario", captor.getValue().motivo());
    }

    @Test
    void confirmadaDuplicada_noRepiteElEventoEnKafka() {
        Atencion a = solicitada();
        a.cambiarEstado(EstadoAtencion.CONFIRMADA, null);
        service.aplicarResultado(a.getId(), AtencionEvent.CONFIRMADA, null);
        verify(publisher, never()).publicarEventoKafka(any());
    }

    @Test
    void atencionInexistente_noPublicaNada() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        service.aplicarResultado(id, AtencionEvent.CONFIRMADA, null);
        verify(publisher, never()).publicarEventoKafka(any());
        verify(publisher, never()).publicarEstado(eq(null));
    }
}
