package ar.edu.utn.tup.solicitudes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.tup.solicitudes.model.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Optional<Estudiante> findByLegajo(String legajo);
}
