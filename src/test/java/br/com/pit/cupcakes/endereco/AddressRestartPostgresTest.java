package br.com.pit.cupcakes.endereco;

import br.com.pit.cupcakes.CupcakesApplication;
import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import br.com.pit.cupcakes.endereco.dto.AddressRequest;
import br.com.pit.cupcakes.endereco.service.AddressService;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@Tag("postgres")
class AddressRestartPostgresTest {
    private ConfigurableApplicationContext start() {
        var application = new SpringApplication(CupcakesApplication.class);
        application.addInitializers(new PostgresTestDatabaseGuard());
        return application.run("--spring.profiles.active=test-pg","--server.port=0");
    }
    @Test void preservesAddressAcrossTwoApplicationContexts() {
        String email = "address-restart-"+UUID.randomUUID()+"@example.com";
        Long addressId;
        try (var first = start()) {
            first.getBean(UserRepository.class).saveAndFlush(new User("Pessoa fictícia",email,"hash-ficticio"));
            addressId = first.getBean(AddressService.class).create(new AddressRequest("Pessoa fictícia","70000-001","Rua fictícia","S/N",null,"Bairro fictício","Brasília","DF",null),email).id();
        }
        try (var second = start()) {
            var jdbc = second.getBean(JdbcTemplate.class);
            try {
                var address = second.getBean(AddressService.class).detail(addressId,email);
                assertThat(address.postalCode()).isEqualTo("70000001"); assertThat(address.number()).isEqualTo("S/N"); assertThat(address.version()).isZero();
            } finally {
                jdbc.update("delete from endereco where id = ? and usuario_id = (select id from usuario where email_normalizado = ?)",addressId,email);
                jdbc.update("delete from usuario where email_normalizado = ?",email);
            }
        }
    }
}
