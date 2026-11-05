package br.com.pit.cupcakes.identidade;

import br.com.pit.cupcakes.CupcakesApplication;
import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("postgres")
class AuthenticationRestartPostgresTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private ServletWebServerApplicationContext start() {
        var application = new SpringApplication(CupcakesApplication.class);
        application.addInitializers(new PostgresTestDatabaseGuard());
        return (ServletWebServerApplicationContext) application.run("--spring.profiles.active=test-pg", "--server.port=0");
    }

    private String base(ServletWebServerApplicationContext context) {
        return "http://localhost:" + context.getWebServer().getPort();
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String url, JsonNode csrf, String type, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", type)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void shouldPersistAccountAcrossRestartAndCompleteCookieLogoutFlow() throws Exception {
        String email = "restart-" + UUID.randomUUID() + "@example.com";
        String password = " senha fictícia ";
        Long userId;
        try (var first = start()) {
            HttpClient client = client();
            var response = get(client, base(first) + "/api/autenticacao/csrf");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("set-cookie").orElseThrow()).contains("HttpOnly", "SameSite=Lax");
            JsonNode csrf = mapper.readTree(response.body());
            var registered = post(client, base(first) + "/api/usuarios", csrf, "application/json",
                    mapper.writeValueAsString(Map.of("nome", "Cliente Reinício", "email", email, "senha", password)));
            assertThat(registered.statusCode()).isEqualTo(201);
            userId = mapper.readTree(registered.body()).get("id").asLong();
        }
        try (var second = start()) {
            try {
                assertThat(second.getBean(UserRepository.class).findByNormalizedEmail(email).orElseThrow().getId()).isEqualTo(userId);
                HttpClient client = client();
                var before = get(client, base(second) + "/api/autenticacao/csrf");
                JsonNode csrf = mapper.readTree(before.body());
                String form = "email=" + URLEncoder.encode(" " + email.toUpperCase(java.util.Locale.ROOT) + " ", StandardCharsets.UTF_8)
                        + "&senha=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
                var login = post(client, base(second) + "/api/autenticacao/login", csrf, "application/x-www-form-urlencoded", form);
                assertThat(login.statusCode()).isEqualTo(200);
                assertThat(login.headers().firstValue("set-cookie").orElseThrow())
                        .doesNotStartWith(before.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0] + ";");
                assertThat(get(client, base(second) + "/api/autenticacao/sessao").statusCode()).isEqualTo(200);
                assertThat(get(client, base(second) + "/api/autenticacao/logout").statusCode()).isEqualTo(404);
                assertThat(get(client, base(second) + "/api/autenticacao/sessao").statusCode()).isEqualTo(200);
                csrf = mapper.readTree(get(client, base(second) + "/api/autenticacao/csrf").body());
                var logout = post(client, base(second) + "/api/autenticacao/logout", csrf, "application/x-www-form-urlencoded", "");
                assertThat(logout.statusCode()).isEqualTo(204);
                assertThat(logout.body()).isEmpty();
                assertThat(get(client, base(second) + "/api/autenticacao/sessao").statusCode()).isEqualTo(401);
                csrf = mapper.readTree(get(client, base(second) + "/api/autenticacao/csrf").body());
                assertThat(post(client, base(second) + "/api/autenticacao/login", csrf, "application/x-www-form-urlencoded", form).statusCode()).isEqualTo(200);
                assertThat(get(client, base(second) + "/api/produtos").statusCode()).isEqualTo(200);
                assertThat(get(client(), base(second) + "/produto.html?id=1").statusCode()).isEqualTo(200);
            } finally {
                second.getBean(JdbcTemplate.class).update("delete from usuario where email_normalizado = ?", email);
            }
        }
    }
}
