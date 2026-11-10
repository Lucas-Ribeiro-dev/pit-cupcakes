package br.com.pit.cupcakes.pedido;

import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.endereco.repository.AddressRepository;
import br.com.pit.cupcakes.frete.entity.ShippingRange;
import br.com.pit.cupcakes.frete.repository.ShippingRangeRepository;
import br.com.pit.cupcakes.identidade.entity.Role;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import br.com.pit.cupcakes.identidade.service.AdminProvisioningService;
import br.com.pit.cupcakes.pedido.repository.OrderAddressRepository;
import br.com.pit.cupcakes.pedido.repository.OrderItemRepository;
import br.com.pit.cupcakes.pedido.repository.OrderRepository;
import br.com.pit.cupcakes.pedido.repository.SimulatedPaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-pg")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgresTestDatabaseGuard.class)
@Tag("postgres")
class OrderAndAdminPostgresIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired AddressRepository addresses;
    @Autowired ShippingRangeRepository shippingRanges;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired OrderAddressRepository orderAddresses;
    @Autowired SimulatedPaymentRepository simulatedPayments;
    @Autowired PasswordEncoder encoder;
    @Autowired AdminProvisioningService adminProvisioning;

    @Test
    void validatesMigrationV4ConstraintsCheckoutOrdersHistoryAndAdminInPostgres() throws Exception {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("cupcakes_test");
        StoreFlowBrowserServer.cleanupFlowFixtures(jdbc, "admin.fluxo.isolado@example.com");
        List<String> versions = jdbc.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class
        );
        assertThat(versions).contains("1", "2", "3", "4");

        String suffix = UUID.randomUUID().toString();
        String customerEmail = "cliente-pg-" + suffix + "@example.com";
        String adminEmail = "admin-pg-" + suffix + "@example.com";
        Long productId = null;
        Long rangeId = null;
        Long addressId = null;
        Long orderId = null;
        Long refusedOrderId = null;

        try {
            User customer = users.saveAndFlush(new User(
                    "Cliente PG",
                    customerEmail,
                    encoder.encode("senha-pg-123"),
                    Role.CLIENTE
            ));
            adminProvisioning.provisionAdmin("Admin PG", adminEmail, "senha-admin-pg-123");
            User admin = users.findByNormalizedEmail(adminEmail).orElseThrow();
            assertThat(admin.getRole()).isEqualTo(Role.ADMIN);

            // E-mail existente como CLIENTE nao e promovido no provisionamento
            assertThatThrownBy(() -> adminProvisioning.provisionAdmin("Cliente PG", customerEmail, "outra-senha-123"))
                    .isInstanceOf(IllegalStateException.class);

            // Admin cria produto via API
            String createdProdJson = mvc.perform(post("/api/admin/produtos")
                            .with(user(adminEmail).roles("ADMIN"))
                            .with(csrf())
                            .contentType("application/json")
                            .content(mapper.writeValueAsString(Map.of(
                                    "nome", "Cupcake PG " + suffix,
                                    "descricao", "Descrição inicial no PostgreSQL",
                                    "preco", "15.00",
                                    "imagemCaminho", "/assets/img/cupcake-baunilha.svg",
                                    "disponivel", true
                            ))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            productId = mapper.readTree(createdProdJson).get("id").asLong();

            // Faixa ficticia isolada para teste
            ShippingRange range = shippingRanges.saveAndFlush(new ShippingRange("70888000", "70888999", new BigDecimal("8.00"), true));
            rangeId = range.getId();

            // Cliente cria endereco
            String addrJson = mvc.perform(post("/api/conta/enderecos")
                            .with(user(customerEmail).roles("CLIENTE"))
                            .with(csrf())
                            .contentType("application/json")
                            .content(mapper.writeValueAsString(Map.of(
                                    "destinatario", "Cliente Destinatário PG",
                                    "cep", "70888-100",
                                    "logradouro", "SQN 888 Bloco A",
                                    "numero", "101",
                                    "bairro", "Asa Norte",
                                    "cidade", "Brasília",
                                    "uf", "DF"
                            ))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            JsonNode addrNode = mapper.readTree(addrJson);
            addressId = addrNode.get("id").asLong();

            MockHttpSession session = new MockHttpSession();
            mvc.perform(post("/api/carrinho/itens")
                            .session(session)
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"produtoId\":" + productId + ",\"quantidade\":2}"))
                    .andExpect(status().isOk());

            // 1. Revisao e recusa simulada
            String revRefused = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                            .session(session)
                            .with(user(customerEmail).roles("CLIENTE"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + addressId
                                    + ",\"versaoEndereco\":" + addrNode.get("versao").asLong() + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value("38.00"))
                    .andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

            String refusedOrderJson = mvc.perform(post("/api/conta/pedidos")
                            .session(session)
                            .with(user(customerEmail).roles("CLIENTE"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"revisaoId\":\"" + revRefused + "\",\"resultadoSimulado\":\"RECUSADO\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statusAtendimento").value("NAO_AUTORIZADO"))
                    .andReturn().getResponse().getContentAsString();
            refusedOrderId = mapper.readTree(refusedOrderJson).get("id").asLong();

            // 2. Nova revisao e confirmacao aprovada + repeticao idempotente
            String revApproved = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                            .session(session)
                            .with(user(customerEmail).roles("CLIENTE"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + addressId
                                    + ",\"versaoEndereco\":" + addrNode.get("versao").asLong() + "}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

            String approvedOrderJson = mvc.perform(post("/api/conta/pedidos")
                            .session(session)
                            .with(user(customerEmail).roles("CLIENTE"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"revisaoId\":\"" + revApproved + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statusAtendimento").value("RECEBIDO"))
                    .andExpect(jsonPath("$.total").value("38.00"))
                    .andReturn().getResponse().getContentAsString();
            orderId = mapper.readTree(approvedOrderJson).get("id").asLong();

            // Repeticao idempotente retorna 200
            mvc.perform(post("/api/conta/pedidos")
                            .session(session)
                            .with(user(customerEmail).roles("CLIENTE"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"revisaoId\":\"" + revApproved + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orderId));

            // 3. Restricoes do banco V4 em PostgreSQL (total = subtotal + frete, retirada com frete zero, FK preserva historico)
            final Long finalOrderId = orderId;
            final Long finalProductId = productId;
            assertThatThrownBy(() -> jdbc.update("update pedido set total = 999.00 where id = ?", finalOrderId))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("update pedido set modalidade = 'RETIRADA' where id = ?", finalOrderId))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("delete from produto where id = ?", finalProductId))
                    .isInstanceOf(DataIntegrityViolationException.class);

            // 4. Edicao do produto no catalogo e atendimento pelo ADMIN ate CONCLUIDO
            mvc.perform(put("/api/admin/produtos/" + productId)
                            .with(user(adminEmail).roles("ADMIN"))
                            .with(csrf())
                            .contentType("application/json")
                            .content(mapper.writeValueAsString(Map.of(
                                    "nome", "Cupcake PG Editado " + suffix,
                                    "descricao", "Descrição alterada",
                                    "preco", "25.00",
                                    "disponivel", true,
                                    "versao", 0
                            ))))
                    .andExpect(status().isOk());

            mvc.perform(put("/api/admin/pedidos/" + orderId + "/status")
                            .with(user(adminEmail).roles("ADMIN"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"statusAtendimento\":\"EM_PREPARACAO\",\"versao\":0}"))
                    .andExpect(status().isOk());
            mvc.perform(put("/api/admin/pedidos/" + orderId + "/status")
                            .with(user(adminEmail).roles("ADMIN"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"statusAtendimento\":\"SAIU_PARA_ENTREGA\",\"versao\":1}"))
                    .andExpect(status().isOk());
            mvc.perform(put("/api/admin/pedidos/" + orderId + "/status")
                            .with(user(adminEmail).roles("ADMIN"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"statusAtendimento\":\"CONCLUIDO\",\"versao\":2}"))
                    .andExpect(status().isOk());

            // 5. Cliente consulta pedido com nome/preco historicos preservados e status CONCLUIDO
            mvc.perform(get("/api/conta/pedidos/" + orderId)
                            .with(user(customerEmail).roles("CLIENTE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusAtendimento").value("CONCLUIDO"))
                    .andExpect(jsonPath("$.itens[0].nome").value("Cupcake PG " + suffix))
                    .andExpect(jsonPath("$.itens[0].precoUnitario").value("15.00"));
        } finally {
            for (Long oid : new Long[]{orderId, refusedOrderId}) {
                if (oid != null) {
                    jdbc.update("delete from pagamento_simulado where pedido_id = ?", oid);
                    jdbc.update("delete from endereco_pedido where pedido_id = ?", oid);
                    jdbc.update("delete from item_pedido where pedido_id = ?", oid);
                    jdbc.update("delete from pedido where id = ?", oid);
                }
            }
            if (addressId != null) jdbc.update("delete from endereco where id = ?", addressId);
            if (rangeId != null) jdbc.update("delete from faixa_frete where id = ?", rangeId);
            if (productId != null) jdbc.update("delete from produto where id = ?", productId);
            jdbc.update("delete from usuario where email_normalizado in (?, ?)", customerEmail, adminEmail);
        }
    }
}

