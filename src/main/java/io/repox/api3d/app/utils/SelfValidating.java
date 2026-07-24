package io.repox.api3d.app.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

public interface SelfValidating<T> {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    @JsonIgnore
    default void selfValidate() throws ConstraintViolationException {
        Set<ConstraintViolation<T>> violations = factory.getValidator().validate(((T) this));
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @JsonIgnore
    default boolean isValid() {
        Set<ConstraintViolation<T>> violations = factory.getValidator().validate(((T) this));
        return violations.isEmpty();
    }
}
