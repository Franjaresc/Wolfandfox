package dev.franjaresc.wolfandfox.route.adapter.in.web;

import dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException;
import dev.franjaresc.wolfandfox.route.domain.InvalidRouteClosureException;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Maps every failure to RFC 7807 problem details. Details come only from domain messages or fixed
 * text here, never from framework, Jackson or database exceptions, because those can expose
 * internals.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI VALIDATION_ERROR = URI.create("urn:wolfandfox:problem:validation-error");
    private static final URI DUPLICATE = URI.create("urn:wolfandfox:problem:duplicate-route-closure");
    private static final URI NOT_FOUND = URI.create("urn:wolfandfox:problem:route-closure-not-found");
    private static final URI INTERNAL_ERROR = URI.create("urn:wolfandfox:problem:internal-error");

    private static final String INVALID_FIELDS = "One or more fields are invalid";

    record FieldError(String field, String message) {
    }

    @ExceptionHandler(DuplicateRouteClosureException.class)
    ProblemDetail handleDuplicate(DuplicateRouteClosureException exception) {
        return problem(HttpStatus.CONFLICT, DUPLICATE, "Route already closed", exception.getMessage());
    }

    @ExceptionHandler(RouteClosureNotFoundException.class)
    ProblemDetail handleNotFound(RouteClosureNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND, "Route closure not found", exception.getMessage());
    }

    @ExceptionHandler(InvalidRouteClosureException.class)
    ProblemDetail handleInvalid(InvalidRouteClosureException exception) {
        return validationProblem(exception.getMessage(), List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        var errors = exception.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return validationProblem(INVALID_FIELDS, errors);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        LOG.error("Unexpected error", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR,
                "Internal server error",
                "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(exception, validationProblem(INVALID_FIELDS, errors), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return respond(exception, validationProblem(INVALID_FIELDS, errors), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = List.of(new FieldError(exception.getPropertyName(), "has an invalid value"));
        return respond(exception, validationProblem(INVALID_FIELDS, errors), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var body = exception.getCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()
                ? validationProblem(INVALID_FIELDS, List.of(toFieldError(jackson)))
                : validationProblem("The request body is missing or is not valid JSON", List.of());
        return respond(exception, body, headers, request);
    }

    private ResponseEntity<Object> respond(
            Exception exception, ProblemDetail body, HttpHeaders headers, WebRequest request) {
        return handleExceptionInternal(exception, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static FieldError toFieldError(JacksonException exception) {
        var field = exception.getPath().stream()
                .map(reference -> reference.getPropertyName() != null
                        ? reference.getPropertyName()
                        : String.valueOf(reference.getIndex()))
                .collect(Collectors.joining("."));
        var message = exception instanceof UnrecognizedPropertyException
                ? "is not a recognized field"
                : "has an invalid value";
        return new FieldError(field, message);
    }

    private static FieldError toFieldError(ConstraintViolation<?> violation) {
        String field = null;
        for (var node : violation.getPropertyPath()) {
            field = node.getName();
        }
        return new FieldError(field, violation.getMessage());
    }

    private static ProblemDetail validationProblem(String detail, List<FieldError> errors) {
        var problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Invalid request", detail);
        problem.setProperty(
                "errors",
                errors.stream().sorted(Comparator.comparing(FieldError::field)).toList());
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        return problem;
    }
}
