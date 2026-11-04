package ar.edu.utn.tup.solicitudes.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ar.edu.utn.tup.solicitudes.dominio.Solicitud;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    // Con open-in-view=false la sesión se cierra al terminar el service. Estos métodos traen estudiante y
    // responsable en la misma consulta para que el mapper web no dispare una LazyInitializationException.

    @EntityGraph(attributePaths = { "estudiante", "responsable" })
    Optional<Solicitud> findDetalleById(Long solicitudId);

    @EntityGraph(attributePaths = { "estudiante", "responsable" })
    @Query("select s from Solicitud s")
    List<Solicitud> findAllConDetalle();
}
