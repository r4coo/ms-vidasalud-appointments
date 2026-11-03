package cl.vidasalud.appointments.repository;

import cl.vidasalud.appointments.model.Atencion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtencionRepository extends JpaRepository<Atencion, UUID> {
    List<Atencion> findByPacienteOrderByFechaAscHoraAsc(String paciente);
    List<Atencion> findAllByOrderByFechaAscHoraAsc();
}
