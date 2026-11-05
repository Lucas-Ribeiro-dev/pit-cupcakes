package br.com.pit.cupcakes.identidade.service;

import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class IdentityValidation {
    private IdentityValidation() {}

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String email, Validator validator) {
        return validator.validate(new EmailValue(email)).isEmpty();
    }

    public static void validatePassword(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 8
                || password.codePointCount(0, password.length()) > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("A senha deve ter de 8 a 64 caracteres Unicode e no máximo 72 bytes UTF-8.");
        }
    }

    private record EmailValue(@NotBlank @Email @Size(max = 254) String email) {}
}
