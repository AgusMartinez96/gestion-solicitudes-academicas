package ar.edu.utn.tup.solicitudes.dominio;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.ReglaDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private TipoSolicitud tipo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDateTime fechaVencimiento;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Responsable responsable;

    public Solicitud() {
    }

    public boolean estaCerrada() {
        return estado.esCerrado();
    }

    public boolean estaVencida(LocalDateTime ahora) {
        return !estaCerrada() && fechaVencimiento.isBefore(ahora);
    }

    public boolean tieneResponsable() {
        return responsable != null;
    }

    public void cambiarEstado(EstadoSolicitud nuevo, LocalDateTime ahora) {
        if (!estado.puedeTransicionarA(nuevo)) {
            throw new ReglaDeNegocioException("Transición inválida: " + estado + " -> " + nuevo);
        }
        if (nuevo.requiereResponsable() && !tieneResponsable()) {
            throw new ReglaDeNegocioException(
                    "La solicitud requiere un responsable asignado para pasar a " + nuevo);
        }
        this.estado = nuevo;
        this.fechaActualizacion = ahora;
    }

    public void verificarQueAdmiteResponsable() {
        if (estaCerrada()) {
            throw new ReglaDeNegocioException("La solicitud está cerrada: " + id);
        }
    }

    public void asignarResponsable(Responsable nuevoResponsable, LocalDateTime ahora) {
        verificarQueAdmiteResponsable();
        if (!nuevoResponsable.isActivo()) {
            throw new ReglaDeNegocioException("El responsable no está activo: " + nuevoResponsable.getId());
        }
        this.responsable = nuevoResponsable;
        if (estado == EstadoSolicitud.PENDIENTE) {
            cambiarEstado(EstadoSolicitud.EN_ANALISIS, ahora);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitud tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Responsable getResponsable() {
        return responsable;
    }

    public void setResponsable(Responsable responsable) {
        this.responsable = responsable;
    }
}
