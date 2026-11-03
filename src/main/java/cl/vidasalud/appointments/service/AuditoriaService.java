package cl.vidasalud.appointments.service;

import cl.vidasalud.appointments.dto.AtencionEvent;
import cl.vidasalud.appointments.model.Auditoria;
import cl.vidasalud.appointments.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repository;
    private final AtencionService atencionService;

    public AuditoriaService(AuditoriaRepository repository, AtencionService atencionService) {
        this.repository = repository;
        this.atencionService = atencionService;
    }

    /** Registra el evento y proyecta el resultado del catálogo sobre la atención, en una transacción. */
    @Transactional
    public void registrar(String routingKey, String payload, AtencionEvent event) {
        repository.save(new Auditoria(routingKey, payload));
        atencionService.aplicarResultado(event.atencionId(), event.tipo(), event.motivo());
    }
}
