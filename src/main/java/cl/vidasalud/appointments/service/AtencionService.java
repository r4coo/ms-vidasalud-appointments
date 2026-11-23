package cl.vidasalud.appointments.service;

import cl.vidasalud.appointments.dto.AtencionEvent;
import cl.vidasalud.appointments.dto.AtencionRequest;
import cl.vidasalud.appointments.dto.Caller;
import cl.vidasalud.appointments.dto.ConfirmarAtencionCommand;
import cl.vidasalud.appointments.exception.BusinessRuleException;
import cl.vidasalud.appointments.exception.ForbiddenException;
import cl.vidasalud.appointments.exception.NotFoundException;
import cl.vidasalud.appointments.messaging.AtencionPublisher;
import cl.vidasalud.appointments.model.Atencion;
import cl.vidasalud.appointments.model.EstadoAtencion;
import cl.vidasalud.appointments.repository.AtencionRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtencionService {
    private final AtencionRepository repository;
    private final AtencionPublisher publisher;
    private final Clock clock;

    public AtencionService(AtencionRepository repository, AtencionPublisher publisher, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    /** Crea la atención (SOLICITADA). Si la regla falla no se persiste ni se publica nada. */
    @Transactional
    public Atencion crear(Caller caller, AtencionRequest req) {
        if (req.fecha().isBefore(LocalDate.now(clock))) {
            throw new BusinessRuleException("La fecha de la atención no puede ser pasada");
        }
        if (req.fecha().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessRuleException("No hay atención los domingos");
        }
        Atencion a = repository.save(new Atencion(UUID.randomUUID(), caller.email(), req.prestacionId(),
                req.prestacion(), req.fecha(), req.hora()));
        publisher.publicarEstado(evento(a, null));
        return a;
    }

    /**
     * Cambio de estado. CONFIRMADA es asíncrono: se publica el comando y el estado cambia cuando el
     * catálogo confirma el cupo. El resto de las transiciones se aplican de inmediato y se publica un evento.
     */
    @Transactional
    public Atencion cambiarEstado(UUID id, EstadoAtencion nuevo, Caller caller) {
        Atencion a = repository.findById(id).orElseThrow(() -> new NotFoundException("Atención no encontrada"));

        if (!caller.isStaff()) {
            // El paciente solo puede cancelar sus propias atenciones.
            if (!a.getPaciente().equalsIgnoreCase(caller.email()) || nuevo != EstadoAtencion.CANCELADA) {
                throw new ForbiddenException("Solo Recepcionista o Admin pueden cambiar este estado");
            }
        }
        if (!a.getEstado().puedePasarA(nuevo)) {
            throw new BusinessRuleException("Transición inválida: " + a.getEstado() + " → " + nuevo
                    + (nuevo == EstadoAtencion.EN_ATENCION ? " (la atención debe estar confirmada y en espera)" : ""));
        }

        if (nuevo == EstadoAtencion.CONFIRMADA) {
            publisher.publicarComando(new ConfirmarAtencionCommand(a.getId(), a.getPaciente(), a.getPrestacionId(),
                    a.getPrestacion(), a.getFecha(), a.getHora()));
            return a; // sigue SOLICITADA hasta que el catálogo confirme el cupo
        }
        a.cambiarEstado(nuevo, null);
        repository.save(a);
        publisher.publicarEstado(evento(a, null));
        return a;
    }

    @Transactional(readOnly = true)
    public List<Atencion> listar(Caller caller) {
        return caller.isStaff() ? repository.findAllByOrderByFechaAscHoraAsc()
                : repository.findByPacienteOrderByFechaAscHoraAsc(caller.email());
    }

    @Transactional(readOnly = true)
    public Atencion obtener(UUID id, Caller caller) {
        Atencion a = repository.findById(id).orElseThrow(() -> new NotFoundException("Atención no encontrada"));
        if (!caller.isStaff() && !a.getPaciente().equalsIgnoreCase(caller.email())) {
            throw new NotFoundException("Atención no encontrada");
        }
        return a;
    }

    /** Resultado publicado por el catálogo (evento atencion.confirmada / atencion.rechazada). */
    @Transactional
    public void aplicarResultado(UUID atencionId, String tipo, String motivo) {
        repository.findById(atencionId).ifPresent(a -> {
            if (AtencionEvent.CONFIRMADA.equals(tipo) && a.getEstado() == EstadoAtencion.SOLICITADA) {
                a.cambiarEstado(EstadoAtencion.CONFIRMADA, null);
                publisher.publicarEventoKafka(resultado(a, AtencionEvent.CONFIRMADA, null));
            } else if (AtencionEvent.RECHAZADA.equals(tipo)) {
                a.registrarMotivo(motivo);
                publisher.publicarEventoKafka(resultado(a, AtencionEvent.RECHAZADA, motivo));
            }
        });
    }

    private static AtencionEvent resultado(Atencion a, String tipo, String motivo) {
        return new AtencionEvent(a.getId(), tipo, a.getEstado().name(), motivo, a.getPaciente(),
                a.getPrestacion(), a.getFecha(), a.getHora());
    }

    private static AtencionEvent evento(Atencion a, String motivo) {
        return new AtencionEvent(a.getId(), AtencionEvent.ESTADO, a.getEstado().name(), motivo, a.getPaciente(),
                a.getPrestacion(), a.getFecha(), a.getHora());
    }
}
