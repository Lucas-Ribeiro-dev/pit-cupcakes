package br.com.pit.cupcakes.config;

import br.com.pit.cupcakes.compartilhado.exception.ErrorResponse;
import br.com.pit.cupcakes.identidade.dto.UserResponse;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import br.com.pit.cupcakes.identidade.service.IdentityValidation;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public LoginAttemptGuard loginAttemptGuard(
            @Value("${pit.security.login.max-attempts:10}") int maxAttempts,
            @Value("${pit.security.login.window-seconds:300}") long windowSeconds) {
        return new LoginAttemptGuard(maxAttempts, Duration.ofSeconds(windowSeconds));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper,
                                                   UserRepository repository, LoginAttemptGuard loginAttemptGuard) throws Exception {
        http.authorizeHttpRequests(access -> access
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/", "/index.html", "/produto.html", "/assets/**", "/paginas/conta/cadastro.html",
                        "/paginas/conta/login.html", "/paginas/conta/minha-conta.html", "/paginas/conta/enderecos.html",
                        "/paginas/conta/pedidos.html", "/paginas/conta/pedido.html",
                        "/paginas/compra/carrinho.html", "/paginas/compra/checkout.html",
                        "/admin/index.html", "/admin/produtos.html", "/admin/pedidos.html",
                        "/api/carrinho", "/api/produtos", "/api/produtos/*", "/api/autenticacao/csrf").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/usuarios", "/api/autenticacao/login", "/api/carrinho/itens").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/carrinho/itens/*").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/carrinho/itens/*").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/conta/**", "/api/autenticacao/sessao").authenticated()
                .anyRequest().authenticated())
            .requestCache(cache -> cache.disable())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                        writeError(mapper, response, 401, "NAO_AUTENTICADO", "É necessário entrar na conta."))
                .accessDeniedHandler((request, response, exception) -> {
                    boolean csrf = exception instanceof CsrfException;
                    writeError(mapper, response, 403, csrf ? "CSRF_INVALIDO" : "ACESSO_NEGADO",
                            csrf ? "Token de segurança ausente ou inválido. Atualize e tente novamente." : "Acesso não permitido.");
                }))
            .formLogin(login -> login.loginPage("/paginas/conta/login.html")
                .loginProcessingUrl("/api/autenticacao/login")
                .usernameParameter("email").passwordParameter("senha")
                .successHandler((request, response, authentication) -> {
                    loginAttemptGuard.reset(attemptKey(request));
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    mapper.writeValue(response.getWriter(), UserResponse.from(
                            repository.findByNormalizedEmail(authentication.getName()).orElseThrow()));
                })
                .failureHandler((request, response, exception) -> {
                    loginAttemptGuard.recordFailure(attemptKey(request));
                    writeError(mapper, response, 401, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.");
                }))
            .logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/api/autenticacao/logout", "POST"))
                .invalidateHttpSession(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .addFilterBefore(new LoginRequestFilter(mapper, loginAttemptGuard), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static String attemptKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr() == null ? "local" : request.getRemoteAddr();
        String email = IdentityValidation.normalizeEmail(request.getParameter("email"));
        return ip + "|" + email;
    }

    private static void writeError(ObjectMapper mapper, HttpServletResponse response, int status,
                                   String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }

    private static class LoginRequestFilter extends OncePerRequestFilter {
        private final ObjectMapper mapper;
        private final LoginAttemptGuard guard;
        LoginRequestFilter(ObjectMapper mapper, LoginAttemptGuard guard) {
            this.mapper = mapper;
            this.guard = guard;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            if ("POST".equals(request.getMethod())
                    && (request.getContextPath() + "/api/autenticacao/login").equals(request.getRequestURI())) {
                String contentType = request.getContentType();
                if (contentType == null || !MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(contentType.split(";", 2)[0].strip())) {
                    writeError(mapper, response, 415, "FORMATO_NAO_SUPORTADO", "Use formulário application/x-www-form-urlencoded.");
                    return;
                }
                String key = attemptKey(request);
                if (!guard.isAllowed(key)) {
                    writeError(mapper, response, 429, "MUITAS_TENTATIVAS_LOGIN",
                            "Muitas tentativas de login falharam. Aguarde alguns minutos antes de tentar novamente.");
                    return;
                }
                try {
                    IdentityValidation.validatePassword(request.getParameter("senha"));
                } catch (ValidationException exception) {
                    guard.recordFailure(key);
                    writeError(mapper, response, 401, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.");
                    return;
                }
                chain.doFilter(new HttpServletRequestWrapper(request) {
                    @Override
                    public String getParameter(String name) {
                        return "email".equals(name) ? IdentityValidation.normalizeEmail(super.getParameter(name)) : super.getParameter(name);
                    }
                }, response);
                return;
            }
            chain.doFilter(request, response);
        }
    }

    // Protecao leve em memoria contra abuso de tentativas consecutivas de login por IP e e-mail
    public static class LoginAttemptGuard {
        private final int maxAttempts;
        private final Duration window;
        private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

        public LoginAttemptGuard(int maxAttempts, Duration window) {
            this.maxAttempts = Math.max(1, maxAttempts);
            this.window = window;
        }

        public boolean isAllowed(String key) {
            AttemptWindow current = attempts.get(key);
            if (current == null) return true;
            if (Instant.now().isAfter(current.expiresAt())) {
                attempts.remove(key, current);
                return true;
            }
            return current.count() < maxAttempts;
        }

        public void recordFailure(String key) {
            Instant now = Instant.now();
            if (attempts.size() > 5000) {
                attempts.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
            }
            attempts.compute(key, (k, existing) -> {
                if (existing == null || now.isAfter(existing.expiresAt())) {
                    return new AttemptWindow(1, now.plus(window));
                }
                return new AttemptWindow(existing.count() + 1, existing.expiresAt());
            });
        }

        public void reset(String key) {
            attempts.remove(key);
        }

        public void clearAll() {
            attempts.clear();
        }

        private record AttemptWindow(int count, Instant expiresAt) {}
    }
}
