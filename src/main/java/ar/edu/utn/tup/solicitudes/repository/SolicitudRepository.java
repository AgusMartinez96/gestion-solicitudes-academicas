package ar.edu.utn.tup.solicitudes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.tup.solicitudes.model.Solicitud;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    @Override
    @EntityGraph(attributePaths = { "estudiante", "responsable" })
    Optional<Solicitud> findById(Long id);

    @Override
    @EntityGraph(attributePaths = { "estudiante", "responsable" })
    List<Solicitud> findAll();
}
