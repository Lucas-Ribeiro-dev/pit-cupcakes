package br.com.pit.cupcakes.pedido;

import br.com.pit.cupcakes.CupcakesApplication;
import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.frete.entity.ShippingRange;
import br.com.pit.cupcakes.frete.repository.ShippingRangeRepository;
import br.com.pit.cupcakes.identidade.service.AdminProvisioningService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class StoreFlowBrowserServer {

    public static void main(String[] args) {
        var application = new SpringApplication(CupcakesApplication.class);
        application.addInitializers(new PostgresTestDatabaseGuard());
        var context = application.run(Stream.concat(
                Stream.of("--spring.profiles.active=test-pg", "--server.port=8094"),
                Stream.of(args)
        ).toArray(String[]::new));

        var products = context.getBean(ProductRepository.class);
        var shippingRanges = context.getBean(ShippingRangeRepository.class);
        var adminProvisioning = context.getBean(AdminProvisioningService.class);
        var jdbc = context.getBean(JdbcTemplate.class);

        String adminEmail = System.getenv().getOrDefault("TEST_BROWSER_ADMIN_EMAIL", "admin.fluxo.isolado@example.com");
        String adminPassword = System.getenv().getOrDefault("TEST_BROWSER_ADMIN_PASSWORD", "AdminFluxo#2026");

        cleanupFlowFixtures(jdbc, adminEmail);
        adminProvisioning.provisionAdmin("Administrador Fluxo Isolado", adminEmail, adminPassword);

        List<Long> createdRangeIds = new ArrayList<>();
        if (shippingRanges.count() == 0) {
            createdRangeIds.add(shippingRanges.save(new ShippingRange("70000000", "70999999", new BigDecimal("8.00"), true)).getId());
            createdRangeIds.add(shippingRanges.save(new ShippingRange("71000000", "71999999", new BigDecimal("12.00"), true)).getId());
        }

        long fixtureProductId = products.save(new Product(
                "Cupcake Fluxo Integrado " + UUID.randomUUID().toString().substring(0, 8),
                "Fixture isolada para verificação integrada do carrinho, checkout e pedidos.",
                new BigDecimal("12.50"),
                "/assets/img/cupcake-baunilha.svg",
                true
        )).getId();

        context.addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent) {
                try {
                    cleanupFlowFixtures(jdbc, adminEmail);
                    if (products.existsById(fixtureProductId)) {
                        products.deleteById(fixtureProductId);
                    }
                    for (Long rid : createdRangeIds) {
                        if (shippingRanges.existsById(rid)) {
                            shippingRanges.deleteById(rid);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    public static void cleanupFlowFixtures(JdbcTemplate jdbc, String adminEmail) {
        jdbc.update("""
                delete from pagamento_simulado where pedido_id in (
                    select id from pedido where usuario_id in (
                        select id from usuario where email_normalizado like 'fluxo-%@example.com'
                    )
                )
                """);
        jdbc.update("""
                delete from endereco_pedido where pedido_id in (
                    select id from pedido where usuario_id in (
                        select id from usuario where email_normalizado like 'fluxo-%@example.com'
                    )
                )
                """);
        jdbc.update("""
                delete from item_pedido where pedido_id in (
                    select id from pedido where usuario_id in (
                        select id from usuario where email_normalizado like 'fluxo-%@example.com'
                    )
                )
                """);
        jdbc.update("""
                delete from pedido where usuario_id in (
                    select id from usuario where email_normalizado like 'fluxo-%@example.com'
                )
                """);
        jdbc.update("""
                delete from endereco where usuario_id in (
                    select id from usuario where email_normalizado like 'fluxo-%@example.com'
                )
                """);
        jdbc.update("delete from produto where nome like 'Cupcake Admin Fluxo%' or nome like 'Cupcake Fluxo Integrado%'");
        jdbc.update("delete from faixa_frete where (cep_inicio = '70000000' and cep_fim = '70999999') or (cep_inicio = '71000000' and cep_fim = '71999999')");
        jdbc.update("delete from usuario where email_normalizado like 'fluxo-%@example.com' or email_normalizado = ?", adminEmail);
    }
}

