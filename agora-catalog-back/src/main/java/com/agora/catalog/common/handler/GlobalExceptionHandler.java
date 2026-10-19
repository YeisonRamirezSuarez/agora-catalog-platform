package com.agora.catalog.common.handler;

import com.agora.catalog.common.dto.ErrorResponse;
import com.agora.catalog.common.exception.DuplicateResourceException;
import com.agora.catalog.common.exception.ResourceInUseException;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return ErrorResponse.of("Datos inválidos", message);
    }

    // Un recurso de otra empresa también responde 404: no revela que existe.
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ErrorResponse.of("No encontrado", ex.getMessage());
    }

    // Una ruta que no existe es 404, no 500.
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoRoute(NoResourceFoundException ex) {
        return ErrorResponse.of("No encontrado", "La ruta no existe");
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ErrorResponse.of("Recurso duplicado", ex.getMessage());
    }

    @ExceptionHandler(ResourceInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInUse(ResourceInUseException ex) {
        log.warn("Resource in use: {}", ex.getMessage());
        return ErrorResponse.of("Recurso en uso", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ErrorResponse.of("Solicitud inválida", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ErrorResponse.of("Solicitud inválida", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ErrorResponse.of("No autorizado", ex.getMessage());
    }

    // Un parámetro de ruta con tipo o rango inválido (un id no numérico, o uno que
    // desborda Long) es un error del cliente, no del servidor: 400, no 500.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parámetro inválido '{}': {}", ex.getName(), ex.getMessage());
        return ErrorResponse.of("Solicitud inválida", "El parámetro '" + ex.getName() + "' no tiene un valor válido");
    }

    // Un @RequestParam obligatorio que no llega se resuelve antes de @PreAuthorize: sin esto
    // sale como 500 (también a quien no tiene permiso). Es un error del cliente: 400.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Falta el parámetro '{}'", ex.getParameterName());
        return ErrorResponse.of("Solicitud inválida", "Falta el parámetro '" + ex.getParameterName() + "'");
    }

    // Una violación de integridad (p. ej. referenciar un id que no existe) es una entrada
    // inválida del cliente, no una falla interna: 409, no 500. No se expone el detalle de BD.
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return ErrorResponse.of("Operación no válida", "La operación viola una restricción de los datos");
    }

    /** Un archivo más grande que spring.servlet.multipart.max-file-size: error del cliente. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Archivo demasiado grande: {}", ex.getMessage());
        return ErrorResponse.of("Solicitud inválida", "El archivo supera el tamaño máximo permitido.");
    }

    /**
     * Un rol que falta no es un error del servidor. Sin esto, cada petición denegada por
     * @PreAuthorize sale como 500 y el front no distingue "no tienes permiso" de "el servidor
     * se rompió". El accessDeniedHandler de SecurityConfig no cubre este caso: @PreAuthorize
     * lanza dentro de la invocación del método, así que llega hasta acá.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ErrorResponse.of("Acceso denegado", "No tienes permiso para realizar esta acción");
    }

    /**
     * Cuerpo JSON que no se puede leer (roto, o un null en un campo primitivo). Spring lo lee
     * antes de evaluar @PreAuthorize: es error del cliente, 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo ilegible: {}", ex.getMessage());
        return ErrorResponse.of("Solicitud inválida", "El cuerpo de la petición no tiene un formato válido.");
    }

    /** Verbo equivocado sobre una ruta que sí existe: 405, no 500. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ErrorResponse.of("Método no permitido",
            "El método " + ex.getMethod() + " no está disponible en esta ruta");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        log.error("Exception: {}", ex.getMessage(), ex);
        return ErrorResponse.of("Estamos teniendo problemas", "Por favor intenta más tarde");
    }

}
