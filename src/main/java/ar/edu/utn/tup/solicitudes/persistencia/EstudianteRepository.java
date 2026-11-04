package ar.edu.utn.tup.solicitudes.persistencia;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.tup.solicitudes.dominio.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Optional<Estudiante> findByLegajo(String legajo);
}
