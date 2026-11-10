package br.com.pit.cupcakes.frete.demo;

import br.com.pit.cupcakes.frete.entity.ShippingRange;
import br.com.pit.cupcakes.frete.repository.ShippingRangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("demo")
public class DemoShippingRangeLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoShippingRangeLoader.class);

    private final ShippingRangeRepository shippingRangeRepository;

    public DemoShippingRangeLoader(ShippingRangeRepository shippingRangeRepository) {
        this.shippingRangeRepository = shippingRangeRepository;
    }

    @Override
    public void run(String... args) {
        if (shippingRangeRepository.count() > 0) {
            logger.info("Base de dados ja contem faixas de frete. Carga demo de frete ignorada.");
            return;
        }

        // Faixas arbitrarias de demonstracao para Brasilia-DF (perfil demo ativo)
        List<ShippingRange> demoRanges = List.of(
                new ShippingRange("70000000", "70999999", new BigDecimal("8.00"), true),
                new ShippingRange("71000000", "71999999", new BigDecimal("12.00"), true)
        );

        shippingRangeRepository.saveAll(demoRanges);
        logger.info("Faixas de frete de demonstracao carregadas com sucesso. Total inserido: {}", demoRanges.size());
    }
}

