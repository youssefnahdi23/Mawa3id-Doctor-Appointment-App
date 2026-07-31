package com.mawa3id.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class TunisianPhoneValidator implements ConstraintValidator<TunisianPhone, String> {

    /** 8 national digits, optional +216 country code. Spaces are tolerated and ignored. */
    private static final Pattern PATTERN = Pattern.compile("^(\\+216)?[0-9]{8}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // optional; pair with @NotBlank where required
        }
        return PATTERN.matcher(value.replace(" ", "")).matches();
    }
}
