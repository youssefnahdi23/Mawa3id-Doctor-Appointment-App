package com.mawa3id.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates that a value is a Tunisian phone number: 8 digits, optionally prefixed
 * with {@code +216}. Null and blank are considered valid (combine with
 * {@code @NotBlank} where the value is required).
 */
@Documented
@Constraint(validatedBy = TunisianPhoneValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface TunisianPhone {

    String message() default "Enter a valid Tunisian phone number (8 digits, optionally +216)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
