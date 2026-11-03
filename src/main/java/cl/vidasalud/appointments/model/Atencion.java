package cl.vidasalud.appointments.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "atenciones")
public class Atencion {
    @Id
    private UUID id;
    @Column(nullable = false) private String paciente;
    @Column(name = "prestacion_id", nullable = false) private Long prestacionId;
    @Column(nullable = false) private String prestacion;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false) private LocalTime hora;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoAtencion estado;
    private String motivo;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    @Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;

    protected Atencion() {}

    public Atencion(UUID id, String paciente, Long prestacionId, String prestacion, LocalDate fecha, LocalTime hora) {
        this.id = id;
        this.paciente = paciente;
        this.prestacionId = prestacionId;
        this.prestacion = prestacion;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = EstadoAtencion.SOLICITADA;
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    public void cambiarEstado(EstadoAtencion nuevo, String motivo) {
        this.estado = nuevo;
        this.motivo = motivo;
        this.actualizadoEn = Instant.now();
    }

    public void registrarMotivo(String motivo) {
        this.motivo = motivo;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public String getPaciente() { return paciente; }
    public Long getPrestacionId() { return prestacionId; }
    public String getPrestacion() { return prestacion; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHora() { return hora; }
    public EstadoAtencion getEstado() { return estado; }
    public String getMotivo() { return motivo; }
}
