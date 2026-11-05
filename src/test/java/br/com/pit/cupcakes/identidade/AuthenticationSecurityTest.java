package br.com.pit.cupcakes.identidade;

import br.com.pit.cupcakes.config.SecurityConfig;
import br.com.pit.cupcakes.identidade.controller.AuthenticationController;
import br.com.pit.cupcakes.identidade.controller.UserController;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import br.com.pit.cupcakes.identidade.service.RegistrationService;
import br.com.pit.cupcakes.identidade.service.UserAuthenticationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthenticationController.class, UserController.class})
@Import({SecurityConfig.class, UserAuthenticationService.class, RegistrationService.class})
class AuthenticationSecurityTest {
    private static final String PASSWORD = " senha fictícia ";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder encoder;
    @MockitoBean UserRepository repository;

    @BeforeEach
    void prepareUser() {
        when(repository.findByNormalizedEmail(anyString())).thenReturn(Optional.empty());
        when(repository.findByNormalizedEmail("cliente@example.com")).thenReturn(Optional.of(
                new User("Cliente Teste", "cliente@example.com", encoder.encode(PASSWORD))));
    }

    private MvcResult token(MockHttpSession session) throws Exception {
        var request = get("/api/autenticacao/csrf");
        if (session != null) request.session(session);
        return mvc.perform(request).andExpect(status().isOk()).andReturn();
    }

    private MvcResult login(MvcResult csrf, String email, String password) throws Exception {
        JsonNode body = mapper.readTree(csrf.getResponse().getContentAsString());
        return mvc.perform(post("/api/autenticacao/login")
                .session((MockHttpSession) csrf.getRequest().getSession())
                .header(body.get("headerName").asText(), body.get("token").asText())
                .contentType("application/x-www-form-urlencoded").param("email", email).param("senha", password)).andReturn();
    }

    @Test
    void shouldNormalizeLoginRotateSessionAndPersistAuthentication() throws Exception {
        MvcResult csrf = token(null);
        String previousId = csrf.getRequest().getSession().getId();
        MvcResult result = login(csrf, "  CLIENTE@EXAMPLE.COM  ", PASSWORD);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("CLIENTE", "cliente@example.com").doesNotContain(PASSWORD, "bcrypt", "senha");
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        assertThat(session.getId()).isNotEqualTo(previousId);
        mvc.perform(get("/api/autenticacao/sessao").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Cliente Teste"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void shouldUseGenericCredentialErrors() throws Exception {
        String wrongPassword = login(token(null), "cliente@example.com", "senha errada").getResponse().getContentAsString();
        MvcResult missingUser = login(token(null), "inexistente@example.com", PASSWORD);
        assertThat(missingUser.getResponse().getStatus()).isEqualTo(401);
        assertThat(missingUser.getResponse().getContentAsString()).isEqualTo(wrongPassword).contains("CREDENCIAIS_INVALIDAS");
        assertThat(login(token(null), "cliente@example.com", PASSWORD.strip()).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void shouldRejectMissingAndInvalidCsrfOnEveryPost() throws Exception {
        for (String path : new String[]{"/api/usuarios", "/api/autenticacao/login", "/api/autenticacao/logout"}) {
            mvc.perform(post(path)).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
            mvc.perform(post(path).header("X-CSRF-TOKEN", "invalido"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
        }
    }

    @Test
    void shouldRefreshTokenLogoutAndAllowNewLogin() throws Exception {
        MvcResult oldToken = token(null);
        MvcResult authenticated = login(oldToken, "cliente@example.com", PASSWORD);
        MockHttpSession session = (MockHttpSession) authenticated.getRequest().getSession();
        JsonNode oldBody = mapper.readTree(oldToken.getResponse().getContentAsString());
        mvc.perform(post("/api/autenticacao/logout").session(session)
                        .header(oldBody.get("headerName").asText(), oldBody.get("token").asText()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/autenticacao/logout").session(session));
        mvc.perform(get("/api/autenticacao/sessao").session(session)).andExpect(status().isOk());
        MvcResult refreshed = token(session);
        JsonNode body = mapper.readTree(refreshed.getResponse().getContentAsString());
        mvc.perform(post("/api/autenticacao/logout").session(session)
                        .header(body.get("headerName").asText(), body.get("token").asText()))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/autenticacao/sessao")).andExpect(status().isUnauthorized());
        assertThat(login(token(null), "cliente@example.com", PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void shouldAuthorizeRolesBeforeMissingBusinessRoutes() throws Exception {
        mvc.perform(get("/api/autenticacao/sessao")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO")).andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/api/admin/futuro")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/futuro").with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/futuro").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/conta/futuro")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/conta/futuro").with(user("cliente").roles("CLIENTE"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/desconhecida")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMalformedUnknownAndUnsupportedRegistration() throws Exception {
        MvcResult csrf = token(null);
        JsonNode body = mapper.readTree(csrf.getResponse().getContentAsString());
        for (String json : new String[]{"{", "{\"nome\":\"Teste\",\"email\":\"t@example.com\",\"senha\":\"12345678\",\"perfil\":\"ADMIN\"}",
                "{\"id\":1}", "{\"hash\":\"segredo\"}", "{}"}) {
            mvc.perform(post("/api/usuarios").session((MockHttpSession) csrf.getRequest().getSession())
                    .header(body.get("headerName").asText(), body.get("token").asText())
                    .contentType("application/json").content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo"))));
        }
        mvc.perform(post("/api/usuarios").session((MockHttpSession) csrf.getRequest().getSession())
                        .header(body.get("headerName").asText(), body.get("token").asText()).contentType("text/plain").content("texto"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/autenticacao/login").session((MockHttpSession) csrf.getRequest().getSession())
                        .header(body.get("headerName").asText(), body.get("token").asText()).contentType("application/json").content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shouldReturnCreatedCustomerWithoutAuthenticationAndConflictForNormalizedEmail() throws Exception {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MvcResult csrf = token(null);
        JsonNode body = mapper.readTree(csrf.getResponse().getContentAsString());
        var session = (MockHttpSession) csrf.getRequest().getSession();
        String json = mapper.writeValueAsString(java.util.Map.of("nome", " Cliente ", "email", " NOVO@EXAMPLE.COM ", "senha", PASSWORD));
        mvc.perform(post("/api/usuarios").session(session)
                        .header(body.get("headerName").asText(), body.get("token").asText())
                        .contentType("application/json").content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Cliente"))
                .andExpect(jsonPath("$.email").value("novo@example.com"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.senha_hash").doesNotExist());
        mvc.perform(get("/api/autenticacao/sessao").session(session)).andExpect(status().isUnauthorized());
        when(repository.existsByNormalizedEmail("novo@example.com")).thenReturn(true);
        mvc.perform(post("/api/usuarios").session(session)
                        .header(body.get("headerName").asText(), body.get("token").asText())
                        .contentType("application/json").content(json))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"));
    }

    @Test
    void shouldRejectInvalidRegistrationFieldsWithoutSavingOrEchoingPassword() throws Exception {
        MvcResult csrf = token(null);
        JsonNode body = mapper.readTree(csrf.getResponse().getContentAsString());
        for (var values : java.util.List.of(
                java.util.Map.of("nome", "a", "email", "c@example.com", "senha", PASSWORD),
                java.util.Map.of("nome", "Cliente", "email", "inválido", "senha", PASSWORD),
                java.util.Map.of("nome", "Cliente", "email", "c@example.com", "senha", "a".repeat(65)),
                java.util.Map.of("nome", "Cliente", "email", "c@example.com", "senha", "😀".repeat(19)))) {
            mvc.perform(post("/api/usuarios").session((MockHttpSession) csrf.getRequest().getSession())
                            .header(body.get("headerName").asText(), body.get("token").asText())
                            .contentType("application/json").content(mapper.writeValueAsString(values)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.senha").doesNotExist());
        }
        verify(repository, never()).saveAndFlush(any());
    }
}
