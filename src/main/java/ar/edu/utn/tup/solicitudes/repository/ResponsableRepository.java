package ar.edu.utn.tup.solicitudes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.tup.solicitudes.model.Responsable;

public interface ResponsableRepository extends JpaRepository<Responsable, Long> {
}
