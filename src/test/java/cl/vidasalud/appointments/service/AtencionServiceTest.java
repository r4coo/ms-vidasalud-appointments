package cl.vidasalud.appointments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.vidasalud.appointments.dto.AtencionRequest;
import cl.vidasalud.appointments.dto.Caller;
import cl.vidasalud.appointments.exception.BusinessRuleException;
import cl.vidasalud.appointments.exception.ForbiddenException;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtencionServiceTest {
    private AtencionRepository repo;
    private AtencionPublisher publisher;
    private AtencionService service;
    private final Caller paciente = new Caller("paciente@vidasalud.cl", Set.of("Paciente"));
    private final Caller recepcion = new Caller("recepcion@vidasalud.cl", Set.of("Recepcionista"));

    @BeforeEach
    void setUp() {
        repo = mock(AtencionRepository.class);
        publisher = mock(AtencionPublisher.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneId.of("America/Santiago")); // lunes
        service = new AtencionService(repo, publisher, clock);
    }

    private Atencion atencion(EstadoAtencion estado) {
        Atencion a = new Atencion(UUID.randomUUID(), paciente.email(), 1L, "Medicina general",
                LocalDate.of(2026, 11, 3), LocalTime.of(10, 0));
        if (estado != EstadoAtencion.SOLICITADA) a.cambiarEstado(estado, null);
        when(repo.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void crear_fechaPasada_noPersisteNiPublica() {
        var req = new AtencionRequest(1L, "Medicina general", LocalDate.of(2026, 11, 1), LocalTime.of(10, 0));
        assertThrows(BusinessRuleException.class, () -> service.crear(paciente, req));
        verify(repo, never()).save(any());
        verify(publisher, never()).publicarEstado(any());
    }

    @Test
    void crear_valida_persisteSolicitadaYPublicaEvento() {
        when(repo.save(any(Atencion.class))).thenAnswer(i -> i.getArgument(0));
        var req = new AtencionRequest(1L, "Medicina general", LocalDate.of(2026, 11, 3), LocalTime.of(10, 0));
        Atencion a = service.crear(paciente, req);
        assertEquals(EstadoAtencion.SOLICITADA, a.getEstado());
        verify(publisher).publicarEstado(any());
    }

    @Test
    void noSePuedePasarAEnAtencionSinConfirmar() {
        Atencion a = atencion(EstadoAtencion.SOLICITADA);
        assertThrows(BusinessRuleException.class, () -> service.cambiarEstado(a.getId(), EstadoAtencion.EN_ATENCION, recepcion));
        verify(publisher, never()).publicarEstado(any());
        verify(publisher, never()).publicarComando(any());
    }

    @Test
    void confirmar_publicaComando_yNoCambiaElEstadoTodavia() {
        Atencion a = atencion(EstadoAtencion.SOLICITADA);
        Atencion r = service.cambiarEstado(a.getId(), EstadoAtencion.CONFIRMADA, recepcion);
        verify(publisher).publicarComando(any());
        assertEquals(EstadoAtencion.SOLICITADA, r.getEstado());
    }

    @Test
    void pacienteNoPuedeConfirmar() {
        Atencion a = atencion(EstadoAtencion.SOLICITADA);
        assertThrows(ForbiddenException.class, () -> service.cambiarEstado(a.getId(), EstadoAtencion.CONFIRMADA, paciente));
    }

    @Test
    void pacientePuedeCancelarLaSuya() {
        Atencion a = atencion(EstadoAtencion.SOLICITADA);
        assertEquals(EstadoAtencion.CANCELADA, service.cambiarEstado(a.getId(), EstadoAtencion.CANCELADA, paciente).getEstado());
    }

    @Test
    void aplicarResultado_confirmada_pasaAConfirmada() {
        Atencion a = atencion(EstadoAtencion.SOLICITADA);
        service.aplicarResultado(a.getId(), "CONFIRMADA", null);
        assertEquals(EstadoAtencion.CONFIRMADA, a.getEstado());
    }
}
