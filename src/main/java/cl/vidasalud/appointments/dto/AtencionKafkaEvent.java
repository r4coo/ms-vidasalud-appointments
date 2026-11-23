package cl.vidasalud.appointments.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Evento del tópico Kafka vidasalud.atenciones.eventos (clave = id de la atención, para conservar el orden
 * de los eventos de una misma atención dentro de una partición).
 *
 * tipo: ESTADO (cambio de estado), CONFIRMADA o RECHAZADA (resultado del catálogo).
 * eventId permite a los consumidores descartar duplicados; version permite evolucionar el esquema.
 */
public record AtencionKafkaEvent(String eventId, int version, String tipo, UUID atencionId, String estado,
                                 String motivo, String paciente, String prestacion, LocalDate fecha,
                                 LocalTime hora, Instant ocurridoEn) {
    public static final int VERSION = 1;

    public static AtencionKafkaEvent desde(AtencionEvent e, Instant ahora) {
        return new AtencionKafkaEvent(UUID.randomUUID().toString(), VERSION, e.tipo(), e.atencionId(), e.estado(),
                e.motivo(), e.paciente(), e.prestacion(), e.fecha(), e.hora(), ahora);
    }
}
