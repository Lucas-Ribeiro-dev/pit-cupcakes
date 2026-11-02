package br.com.pit.cupcakes.catalogo.demo;

import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataLoader.class);

    private final ProductRepository productRepository;

    public DemoDataLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            logger.info("Base de dados ja contem produtos. Carga de demonstracao ignorada.");
            return;
        }

        logger.info("Carregando 4 produtos de demonstracao (perfil demo ativo)...");

        List<Product> demoProducts = List.of(
                new Product(
                        "Cupcake de Baunilha Tradicional (Demonstração)",
                        "Cupcake fofinho de baunilha natural de Madagascar com cobertura cremosa de buttercream artesanal.",
                        new BigDecimal("12.50"),
                        "/assets/img/cupcake-baunilha.svg",
                        true
                ),
                new Product(
                        "Cupcake de Chocolate Belga (Demonstração)",
                        "Massa intensa de cacau 70% recheada com ganache trufada e confeitos de chocolate belga.",
                        new BigDecimal("14.00"),
                        "/assets/img/cupcake-chocolate.svg",
                        true
                ),
                new Product(
                        "Cupcake de Frutas Vermelhas (Demonstração)",
                        "Bolo leve aromatizado com raspas de limão e recheio artesanal de morango, amora e framboesa fresca.",
                        new BigDecimal("15.50"),
                        null,
                        true
                ),
                new Product(
                        "Cupcake de Caramelo Salgado (Indisponível - Demonstração)",
                        "Massa amanteigada com recheio cremoso de doce de leite suave e toque especial de flor de sal.",
                        new BigDecimal("13.00"),
                        "/assets/img/cupcake-caramelo.svg",
                        false
                )
        );

        productRepository.saveAll(demoProducts);
        logger.info("Carga de demonstracao concluida com sucesso. Total inserido: {}", demoProducts.size());
    }
}

