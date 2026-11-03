package cl.vidasalud.appointments.dto;

import cl.vidasalud.appointments.model.Atencion;
import java.time.LocalDate;
import java.util.UUID;

public record AtencionResponse(UUID id, Long prestacionId, String prestacion, LocalDate fecha, String hora,
                               String paciente, String estado, String motivo) {
    public static AtencionResponse from(Atencion a) {
        return new AtencionResponse(a.getId(), a.getPrestacionId(), a.getPrestacion(), a.getFecha(),
                String.format("%02d:%02d", a.getHora().getHour(), a.getHora().getMinute()),
                a.getPaciente(), a.getEstado().name(), a.getMotivo());
    }
}
