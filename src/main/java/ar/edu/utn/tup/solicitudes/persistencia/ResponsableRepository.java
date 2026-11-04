package ar.edu.utn.tup.solicitudes.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.tup.solicitudes.dominio.Responsable;

public interface ResponsableRepository extends JpaRepository<Responsable, Long> {
}
