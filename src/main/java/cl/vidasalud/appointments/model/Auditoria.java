package cl.vidasalud.appointments.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auditoria")
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "routing_key", nullable = false) private String routingKey;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(name = "recibido_en", nullable = false) private Instant recibidoEn = Instant.now();

    protected Auditoria() {}

    public Auditoria(String routingKey, String payload) {
        this.routingKey = routingKey;
        this.payload = payload;
    }

    public Long getId() { return id; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public Instant getRecibidoEn() { return recibidoEn; }
}
