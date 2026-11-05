package br.com.pit.cupcakes.identidade;

import br.com.pit.cupcakes.CupcakesApplication;
import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import org.springframework.boot.SpringApplication;
import java.util.stream.Stream;

public class AuthenticationBrowserServer {
    public static void main(String[] args) {
        var application = new SpringApplication(CupcakesApplication.class);
        application.addInitializers(new PostgresTestDatabaseGuard());
        application.run(Stream.concat(Stream.of("--spring.profiles.active=test-pg", "--server.port=8091"),
                Stream.of(args)).toArray(String[]::new));
    }
}
