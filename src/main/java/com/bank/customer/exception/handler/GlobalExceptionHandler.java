package com.bank.customer.exception.handler;

import com.bank.customer.exception.CustomerNotFoundException;
import com.bank.customer.exception.DuplicateLegalIdException;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 {@code application/problem+json} responses.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so that standard Spring MVC
 * failures (missing parameters, unparseable bodies, wrong-typed path variables)
 * also map to the correct 4xx instead of falling through to the 500 catch-all.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleNotFound(CustomerNotFoundException exception) {
        log.debug("customer lookup failed: {}", exception.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Customer not found", exception.getMessage());
    }

    @ExceptionHandler(DuplicateLegalIdException.class)
    public ProblemDetail handleDuplicateLegalId(DuplicateLegalIdException exception) {
        log.warn("rejected duplicate registration: {}", exception.getMessage());
        return problem(HttpStatus.CONFLICT, "Duplicate legal id", exception.getMessage());
    }

    /** Raised by {@code @Valid} on a request body; lists every rejected field at once. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage(),
                        (first, second) -> first));

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Safety net for a constraint violation that slipped past the service-level pre-check. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("database rejected the write", exception);
        return problem(HttpStatus.CONFLICT, "Constraint violation",
                "The request conflicts with data that already exists");
    }

    /** Anything unforeseen: logged in full, reported to the client as a bare 500. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("unhandled exception", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "The request could not be processed");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
