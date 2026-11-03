package cl.vidasalud.appointments.controller;

import cl.vidasalud.appointments.dto.AtencionRequest;
import cl.vidasalud.appointments.dto.AtencionResponse;
import cl.vidasalud.appointments.dto.CambioEstadoRequest;
import cl.vidasalud.appointments.dto.Caller;
import cl.vidasalud.appointments.model.EstadoAtencion;
import cl.vidasalud.appointments.service.AtencionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Expuesto como /api/appointments/*. El BFF autentica y propaga X-User-Email / X-User-Roles. */
@RestController
@RequestMapping("/api/appointments")
public class AtencionController {
    private final AtencionService service;

    public AtencionController(AtencionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AtencionResponse> crear(@RequestHeader("X-User-Email") String email,
                                                  @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                                  @Valid @RequestBody AtencionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AtencionResponse.from(service.crear(Caller.of(email, roles), request)));
    }

    @GetMapping
    public List<AtencionResponse> listar(@RequestHeader("X-User-Email") String email,
                                         @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return service.listar(Caller.of(email, roles)).stream().map(AtencionResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AtencionResponse obtener(@PathVariable UUID id, @RequestHeader("X-User-Email") String email,
                                    @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return AtencionResponse.from(service.obtener(id, Caller.of(email, roles)));
    }

    /** CONFIRMADA responde 202: el cambio de estado ocurre cuando el catálogo confirma el cupo. */
    @PutMapping("/{id}/estado")
    public ResponseEntity<AtencionResponse> cambiarEstado(@PathVariable UUID id,
                                                          @RequestHeader("X-User-Email") String email,
                                                          @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                                          @Valid @RequestBody CambioEstadoRequest request) {
        var atencion = service.cambiarEstado(id, request.estado(), Caller.of(email, roles));
        HttpStatus status = request.estado() == EstadoAtencion.CONFIRMADA ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(AtencionResponse.from(atencion));
    }
}
