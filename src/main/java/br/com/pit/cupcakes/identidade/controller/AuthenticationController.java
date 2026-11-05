package br.com.pit.cupcakes.identidade.controller;

import br.com.pit.cupcakes.identidade.dto.UserResponse;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autenticacao")
public class AuthenticationController {
    private final UserRepository repository;
    public AuthenticationController(UserRepository repository) { this.repository = repository; }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName(), token.getParameterName());
    }

    @GetMapping("/sessao")
    public UserResponse session(Authentication authentication) {
        return UserResponse.from(repository.findByNormalizedEmail(authentication.getName()).orElseThrow());
    }

    public record CsrfResponse(String token, String headerName, String parameterName) {}
}
