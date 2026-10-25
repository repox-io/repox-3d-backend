package io.repox.api3d.app.utils.exception;

import io.repox.api3d.app.utils.ApplicationClock;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ErrorController extends ResponseEntityExceptionHandler {
    private final ApplicationClock clock;

    @ExceptionHandler(value = {ConstraintViolationException.class})
    protected ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request, Authentication authentication) {

        HashMap<Path, String> constraintPathDescriptionPairs = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            constraintPathDescriptionPairs.computeIfPresent(
                    cv.getPropertyPath(), (key, oldValue) -> oldValue + ", " + cv.getMessage());
            constraintPathDescriptionPairs.putIfAbsent(cv.getPropertyPath(), cv.getMessage());
        });
        String message = "Constraint Violations";
        ProblemDetail response =
                getStandardResponse(ex, request, HttpStatus.BAD_REQUEST, message, constraintPathDescriptionPairs);

        log.info(
                "ErrorController - Known Exception - CONSTRAINT VIOLATION EXCEPTION\n\tUser: {}\n\tRequest: {}\n\tResponse: {}",
                userIdFromAuthentication(authentication),
                request,
                response);
        if (log.isDebugEnabled()) {
            log.debug("ErrorController - Known Exception - CONSTRAINT VIOLATION EXCEPTION - Stacktrace", ex);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(value = {KnownRuntimeError.class})
    protected ResponseEntity<ProblemDetail> handleRuntimeExceptionWithCustomMessage(
            KnownRuntimeError ex, WebRequest request, Authentication authentication) {

        HttpStatus status = statusOf(ex);
        ProblemDetail response = getStandardResponse(ex, request, status, ex.getMessage());

        log.info(
                "ErrorController - Known Exception\n\tUser: {}\n\tRequest: {}\n\tResponse: {}",
                userIdFromAuthentication(authentication),
                request,
                response,
                ex);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(value = {AuthenticationException.class})
    protected ResponseEntity<ProblemDetail> handleBadCredentialsExceptionWithCustomMessage(
            AuthenticationException ex, WebRequest request, Authentication authentication) {

        ProblemDetail response = getStandardResponse(ex, request, HttpStatus.FORBIDDEN, ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(value = {RejectedExecutionException.class})
    protected ResponseEntity<ProblemDetail> handleRejectedExecutionException(
            RejectedExecutionException ex, WebRequest request, Authentication authentication) {

        ProblemDetail response = getStandardResponse(
                ex, request, HttpStatus.TOO_MANY_REQUESTS, "Message: Task is running. Please try later");

        log.info(
                "ErrorController - Known Exception\n\tUser: {}\n\tRequest: {}\n\tResponse: {}",
                userIdFromAuthentication(authentication),
                request,
                response,
                ex);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(value = {RuntimeException.class})
    protected ResponseEntity<ProblemDetail> handleRejectedExecutionException(
            RuntimeException ex, WebRequest request, Authentication authentication) {

        ProblemDetail response = getStandardResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());

        log.info(
                "ErrorController - Known Exception\n\tUser: {}\n\tRequest: {}\n\tResponse: {}",
                userIdFromAuthentication(authentication),
                request,
                response,
                ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @Override
    //    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
    // HttpStatus status, WebRequest request) {
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ProblemDetail response = getStandardResponse(ex, request, statusCode, ex.getMessage());

        return super.handleExceptionInternal(ex, response, headers, statusCode, request);
    }

    private ProblemDetail getStandardResponse(
            Exception ex, WebRequest request, HttpStatusCode statusCode, String message, Serializable details) {
        final ProblemDetail problemDetail = ProblemDetail.forStatus(statusCode);
        problemDetail.setTitle(ex.getClass().getSimpleName());
        problemDetail.setDetail(message);
        problemDetail.setProperty("timestamp", Timestamp.valueOf(clock.now()).getTime());
        if (details != null) {
            problemDetail.setProperty("details", details);
        }
        if (request instanceof ServletWebRequest) {
            HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
            problemDetail.setProperty(
                    "full-path",
                    httpRequest.getRequestURL()
                            + Optional.ofNullable(httpRequest.getQueryString())
                                    .map(qs -> "?" + qs)
                                    .orElse(""));
        }
        return problemDetail;
    }

    private ProblemDetail getStandardResponse(
            Exception ex, WebRequest request, HttpStatusCode statusCode, String message) {
        return getStandardResponse(ex, request, statusCode, message, null);
    }

    private String userIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        return authentication.getName();
    }

    /**
     * The status to report for a {@link KnownRuntimeError}: an explicitly carried one wins (it exists precisely
     * because {@link KnownRuntimeError.ErrorType} could not express it, e.g. a status relayed from an upstream
     * host), otherwise the status is derived from the error type. A non-standard explicit code degrades to 500
     * rather than failing the error response itself.
     */
    HttpStatus statusOf(KnownRuntimeError ex) {
        if (ex.httpStatus() == null) {
            return fromKnownErrorType(ex.errorType());
        }
        HttpStatus resolved = HttpStatus.resolve(ex.httpStatus());
        return resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
    }

    HttpStatus fromKnownErrorType(KnownRuntimeError.ErrorType errorType) {
        switch (errorType) {
            case NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case FORBIDDEN:
                return HttpStatus.FORBIDDEN;
            case BAD_REQUEST:
                return HttpStatus.BAD_REQUEST;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
