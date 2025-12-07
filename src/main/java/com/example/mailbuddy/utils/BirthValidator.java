package com.example.mailbuddy.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class BirthValidator implements ConstraintValidator<ValidBirth, String> {
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);

    @Override
    public boolean isValid(String birth, ConstraintValidatorContext context) {
        if (birth == null || birth.isBlank()) return true; // @NotBlank로 체크함
        try {
            LocalDate birthDate = LocalDate.parse(birth); // 형식, 존재하는 날짜까지 자동 체크됨
            LocalDate now = LocalDate.now();
            if (birthDate.isBefore(MIN_DATE)) return false;
            if (birthDate.isAfter(now)) return false;
        } catch (DateTimeParseException e) {
            return false;
        }
        return true;
    }
}

