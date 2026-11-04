package ar.edu.utn.tup.solicitudes.web;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ar.edu.utn.tup.solicitudes.dominio.excepciones.DatoInvalidoException;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.RecursoNoEncontradoException;
import ar.edu.utn.tup.solicitudes.dominio.excepciones.ReglaDeNegocioException;
import ar.edu.utn.tup.solicitudes.web.dto.ErrorResponse;

@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);
    private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=UTF-8");
    private static final String MENSAJE_ERROR_INTERNO = "Error interno del servidor";

    @ExceptionHandler(DatoInvalidoException.class)
    public ResponseEntity<Object> manejarDatoInvalido(DatoInvalidoException excepcion) {
        return responder(HttpStatus.BAD_REQUEST, excepcion.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> manejarNoEncontrado(RecursoNoEncontradoException excepcion) {
        return responder(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Object> manejarReglaDeNegocio(ReglaDeNegocioException excepcion) {
        return responder(HttpStatus.CONFLICT, excepcion.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> manejarInesperada(Exception excepcion) {
        LOG.error("Error inesperado", excepcion);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_ERROR_INTERNO);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception excepcion, Object cuerpo,
            HttpHeaders cabeceras, HttpStatusCode estado, WebRequest peticion) {
        HttpStatus status = HttpStatus.valueOf(estado.value());
        return responder(status, detalleSeguro(status, cuerpo));
    }

    private String detalleSeguro(HttpStatus status, Object cuerpo) {
        if (status.is5xxServerError()) {
            return MENSAJE_ERROR_INTERNO;
        }
        if (cuerpo instanceof ProblemDetail problema && problema.getDetail() != null) {
            return problema.getDetail();
        }
        return status.getReasonPhrase();
    }

    private ResponseEntity<Object> responder(HttpStatus status, String mensaje) {
        ErrorResponse cuerpo = new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje,
                LocalDateTime.now());
        return ResponseEntity.status(status).contentType(JSON_UTF8).body(cuerpo);
    }
}
