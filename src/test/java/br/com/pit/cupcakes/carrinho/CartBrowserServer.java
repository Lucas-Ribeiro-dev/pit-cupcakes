package br.com.pit.cupcakes.carrinho;

import br.com.pit.cupcakes.CupcakesApplication;
import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.context.event.ContextClosedEvent;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

public class CartBrowserServer {
    public static void main(String[] args) {
        var application = new SpringApplication(CupcakesApplication.class);
        application.addInitializers(new PostgresTestDatabaseGuard());
        var context = application.run(Stream.concat(Stream.of("--spring.profiles.active=test-pg", "--server.port=8093"),
                Stream.of(args)).toArray(String[]::new));
        var products = context.getBean(ProductRepository.class);
        long fixtureId = products.save(new Product("Cupcake carrinho " + UUID.randomUUID(),
                "Fixture isolada de navegador", new BigDecimal("12.34"), null, true)).getId();
        context.addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent && products.existsById(fixtureId)) products.deleteById(fixtureId);
        });
    }
}
