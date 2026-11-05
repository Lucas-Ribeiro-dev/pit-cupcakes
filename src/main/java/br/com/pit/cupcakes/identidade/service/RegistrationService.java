package br.com.pit.cupcakes.identidade.service;

import br.com.pit.cupcakes.identidade.dto.RegistrationRequest;
import br.com.pit.cupcakes.identidade.dto.UserResponse;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final Validator validator;

    public RegistrationService(UserRepository repository, PasswordEncoder encoder, Validator validator) {
        this.repository = repository;
        this.encoder = encoder;
        this.validator = validator;
    }

    @Transactional
    public UserResponse register(RegistrationRequest request) {
        String name = request.name() == null ? "" : request.name().strip();
        String email = IdentityValidation.normalizeEmail(request.email());
        int nameLength = name.codePointCount(0, name.length());
        if (nameLength < 2 || nameLength > 120 || !IdentityValidation.isValidEmail(email, validator)) {
            throw new ValidationException("Informe nome de 2 a 120 caracteres e e-mail válido de até 254 caracteres.");
        }
        IdentityValidation.validatePassword(request.password());
        if (repository.existsByNormalizedEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        try {
            return UserResponse.from(repository.saveAndFlush(new User(name, email, encoder.encode(request.password()))));
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && "uk_usuario_email_normalizado".equals(violation.getConstraintName())) {
                    throw new EmailAlreadyRegisteredException();
                }
            }
            throw exception;
        }
    }
}
