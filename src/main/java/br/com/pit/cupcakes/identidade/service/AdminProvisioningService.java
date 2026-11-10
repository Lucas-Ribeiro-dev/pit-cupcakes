package br.com.pit.cupcakes.identidade.service;

import br.com.pit.cupcakes.identidade.dto.UserResponse;
import br.com.pit.cupcakes.identidade.entity.Role;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AdminProvisioningService {

    private static final Logger logger = LoggerFactory.getLogger(AdminProvisioningService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public AdminProvisioningService(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    Validator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional
    public UserResponse provisionAdmin(String rawName, String rawEmail, String rawPassword) {
        String name = rawName == null ? "" : rawName.strip();
        String email = IdentityValidation.normalizeEmail(rawEmail);
        int nameLength = name.codePointCount(0, name.length());
        if (nameLength < 2 || nameLength > 120 || !IdentityValidation.isValidEmail(email, validator)) {
            throw new IllegalStateException("Provisionamento administrativo inválido: informe ADMIN_NAME (2 a 120 caracteres) e ADMIN_EMAIL válido.");
        }
        try {
            IdentityValidation.validatePassword(rawPassword);
        } catch (ValidationException ex) {
            throw new IllegalStateException("Provisionamento administrativo inválido: ADMIN_PASSWORD deve ter de 8 a 64 caracteres Unicode e no máximo 72 bytes UTF-8.");
        }

        Optional<User> existingOpt = userRepository.findByNormalizedEmail(email);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (existing.getRole() != Role.ADMIN) {
                throw new IllegalStateException("Provisionamento administrativo recusado: o e-mail informado já pertence a uma conta CLIENTE e não pode ser promovido automaticamente.");
            }
            if (!existing.getName().equals(name) || !passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
                throw new IllegalStateException("Provisionamento administrativo recusado: conta ADMIN já existe para o e-mail informado e não pode ter senha ou nome sobrescritos automaticamente.");
            }
            logger.info("Conta administrativa ja provisionada para {}. Nenhuma alteracao aplicada.", email);
            return UserResponse.from(existing);
        }

        User admin = new User(name, email, passwordEncoder.encode(rawPassword), Role.ADMIN);
        User saved = userRepository.saveAndFlush(admin);
        logger.info("Conta administrativa provisionada com sucesso para {}.", email);
        return UserResponse.from(saved);
    }
}

