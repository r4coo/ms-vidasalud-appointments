package cl.vidasalud.appointments.dto;

import cl.vidasalud.appointments.model.EstadoAtencion;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull EstadoAtencion estado) {}
