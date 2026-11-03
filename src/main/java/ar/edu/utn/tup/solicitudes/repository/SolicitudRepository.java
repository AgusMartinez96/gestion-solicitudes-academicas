package ar.edu.utn.tup.solicitudes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.tup.solicitudes.model.Solicitud;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
}
