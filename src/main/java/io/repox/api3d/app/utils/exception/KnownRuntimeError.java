package io.repox.api3d.app.utils.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KnownRuntimeError extends RuntimeException {
    private final ErrorType errorType;

    public KnownRuntimeError withErrorType(ErrorType errorType) {
        return new KnownRuntimeError(this.getMessage(), this.getCause(), errorType);
    }

    public enum ErrorType {
        BAD_REQUEST,
        FORBIDDEN,
        NOT_FOUND
    }

    public KnownRuntimeError(String message) {
        super(message);
        this.errorType = ErrorType.BAD_REQUEST;
    }

    public KnownRuntimeError(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.BAD_REQUEST;
    }

    private KnownRuntimeError(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType errorType() {
        return errorType;
    }
}
