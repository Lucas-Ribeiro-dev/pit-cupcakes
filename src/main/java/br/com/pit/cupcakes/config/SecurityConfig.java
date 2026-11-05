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

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper,
                                                   UserRepository repository) throws Exception {
        http.authorizeHttpRequests(access -> access
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/", "/index.html", "/produto.html", "/assets/**", "/paginas/conta/cadastro.html",
                        "/paginas/conta/login.html", "/paginas/conta/minha-conta.html",
                        "/api/produtos", "/api/produtos/*", "/api/autenticacao/csrf").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/usuarios", "/api/autenticacao/login").permitAll()
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
                    response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(response.getWriter(), UserResponse.from(
                            repository.findByNormalizedEmail(authentication.getName()).orElseThrow()));
                })
                .failureHandler((request, response, exception) ->
                        writeError(mapper, response, 401, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.")))
            .logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/api/autenticacao/logout", "POST"))
                .invalidateHttpSession(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .addFilterBefore(new LoginRequestFilter(mapper), UsernamePasswordAuthenticationFilter.class);
        return http.build();
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
        LoginRequestFilter(ObjectMapper mapper) { this.mapper = mapper; }

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
                try {
                    IdentityValidation.validatePassword(request.getParameter("senha"));
                } catch (ValidationException exception) {
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
}
