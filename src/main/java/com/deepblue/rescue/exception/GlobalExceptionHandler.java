package com.deepblue.rescue.exception;

import com.deepblue.rescue.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones a un ErrorResponse uniforme:
 * 400 validacion / JSON invalido, 404 recurso inexistente, 409 regla de negocio.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // Si un campo tiene varios errores, se conserva el primero
            details.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        return build(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request or invalid field value",
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'",
                Map.of(ex.getName(), "Invalid value: " + ex.getValue())
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Required parameter is missing",
                Map.of(ex.getParameterName(), "Parameter is required")
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {

        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException ex) {

        return build(
                HttpStatus.NOT_FOUND,
                "Endpoint not found: /" + ex.getResourcePath(),
                Map.of()
        );
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex) {

        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {

        log.error("Unexpected error", ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            Map<String, String> details) {

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now().withNano(0),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );

        return ResponseEntity.status(status).body(body);
    }
}
