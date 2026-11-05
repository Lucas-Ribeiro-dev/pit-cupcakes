package br.com.pit.cupcakes.identidade;

import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-pg")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgresTestDatabaseGuard.class)
@Tag("postgres")
class AuthenticationPostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository repository;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @Test
    void shouldMigrateRegisterEnforceUniqueEmailAndAuthenticatePersistedCustomer() throws Exception {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("cupcakes_test");
        assertThat(jdbc.queryForList("select version from flyway_schema_history where success = true order by installed_rank", String.class))
                .contains("1", "2");
        String email = "auth-" + UUID.randomUUID() + "@example.com";
        String password = " senha fictícia ";
        var csrfResult = mvc.perform(get("/api/autenticacao/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession();
        JsonNode csrf = mapper.readTree(csrfResult.getResponse().getContentAsString());
        String json = mapper.writeValueAsString(Map.of("nome", " Cliente Teste ", "email", " " + email.toUpperCase(java.util.Locale.ROOT) + " ", "senha", password));
        try {
            mvc.perform(post("/api/usuarios").session(session).header(csrf.get("headerName").asText(), csrf.get("token").asText())
                    .contentType("application/json").content(json))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.perfil").value("CLIENTE"))
                    .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.senha").doesNotExist());
            var user = repository.findByNormalizedEmail(email).orElseThrow();
            assertThat(user.getName()).isEqualTo("Cliente Teste");
            assertThat(user.getPasswordHash()).startsWith("{bcrypt}").isNotEqualTo(password);
            assertThat(encoder.matches(password, user.getPasswordHash())).isTrue();
            mvc.perform(get("/api/autenticacao/sessao").session(session)).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/usuarios").session(session).header(csrf.get("headerName").asText(), csrf.get("token").asText())
                    .contentType("application/json").content(json))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"));
            assertThat(jdbc.queryForObject("select count(*) from usuario where email_normalizado = ?", Integer.class, email)).isEqualTo(1);
            assertThatThrownBy(() -> jdbc.update("insert into usuario (nome,email_normalizado,senha_hash,perfil) values (?,?,?,?)",
                    "Duplicado", email, user.getPasswordHash(), "CLIENTE"))
                    .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
            assertThatThrownBy(() -> jdbc.update("insert into usuario (nome,email_normalizado,senha_hash,perfil) values (?,?,?,?)",
                    "Inválido", "invalid-" + email, user.getPasswordHash(), "OUTRO"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            String previousId = session.getId();
            mvc.perform(post("/api/autenticacao/login").session(session)
                    .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                    .contentType("application/x-www-form-urlencoded").param("email", " " + email.toUpperCase(java.util.Locale.ROOT) + " ").param("senha", password))
                    .andExpect(status().isOk());
            assertThat(session.getId()).isNotEqualTo(previousId);
            mvc.perform(get("/api/autenticacao/sessao").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user.getId()));
        } finally {
            jdbc.update("delete from usuario where email_normalizado = ?", email);
        }
    }
}
