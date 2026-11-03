package cl.vidasalud.appointments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record AtencionRequest(
        @NotNull Long prestacionId,
        @NotBlank @Size(max = 120) String prestacion,
        @NotNull LocalDate fecha,
        @NotNull LocalTime hora) {}
