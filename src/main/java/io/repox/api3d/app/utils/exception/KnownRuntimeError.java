package io.repox.api3d.app.utils.exception;

public class KnownRuntimeError extends RuntimeException {
    private final ErrorType errorType;

    /**
     * An explicit HTTP status that takes precedence over {@link #errorType}, or null to derive the status from
     * the error type. Needed where the status is not one the coarse {@link ErrorType} can express — notably a
     * failure relayed from an upstream host, whose own status is reported to the caller. Kept as a plain
     * {@code int} so this exception stays usable from framework-free application services.
     */
    private final Integer httpStatus;

    public KnownRuntimeError withErrorType(ErrorType errorType) {
        return new KnownRuntimeError(this.getMessage(), this.getCause(), errorType, null);
    }

    /** Builds an error that reports the given HTTP status verbatim, whatever {@link ErrorType} could express. */
    public static KnownRuntimeError withStatus(int httpStatus, String message) {
        return withStatus(httpStatus, message, null);
    }

    /** Builds an error that reports the given HTTP status verbatim, whatever {@link ErrorType} could express. */
    public static KnownRuntimeError withStatus(int httpStatus, String message, Throwable cause) {
        return new KnownRuntimeError(message, cause, ErrorType.BAD_REQUEST, httpStatus);
    }

    public enum ErrorType {
        BAD_REQUEST,
        FORBIDDEN,
        NOT_FOUND
    }

    public KnownRuntimeError(String message) {
        this(message, null, ErrorType.BAD_REQUEST, null);
    }

    public KnownRuntimeError(String message, Throwable cause) {
        this(message, cause, ErrorType.BAD_REQUEST, null);
    }

    private KnownRuntimeError(String message, Throwable cause, ErrorType errorType, Integer httpStatus) {
        super(message, cause);
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }

    public ErrorType errorType() {
        return errorType;
    }

    /** The explicit HTTP status to report, or null when it should be derived from {@link #errorType()}. */
    public Integer httpStatus() {
        return httpStatus;
    }
}
