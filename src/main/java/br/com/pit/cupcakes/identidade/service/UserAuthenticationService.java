package br.com.pit.cupcakes.identidade.service;

import br.com.pit.cupcakes.identidade.repository.UserRepository;
import jakarta.validation.Validator;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthenticationService implements UserDetailsService {
    private final UserRepository repository;
    private final Validator validator;

    public UserAuthenticationService(UserRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String email = IdentityValidation.normalizeEmail(username);
        if (!IdentityValidation.isValidEmail(email, validator)) {
            throw new UsernameNotFoundException("Credenciais inválidas.");
        }
        var user = repository.findByNormalizedEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return org.springframework.security.core.userdetails.User.withUsername(email)
                .password(user.getPasswordHash()).roles(user.getRole().name()).build();
    }
}
