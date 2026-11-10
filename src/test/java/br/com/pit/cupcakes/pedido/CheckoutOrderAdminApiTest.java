package br.com.pit.cupcakes.pedido;

import br.com.pit.cupcakes.carrinho.service.CartService;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.config.SecurityConfig;
import br.com.pit.cupcakes.endereco.repository.AddressRepository;
import br.com.pit.cupcakes.frete.demo.DemoShippingRangeLoader;
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
import br.com.pit.cupcakes.pedido.service.CheckoutService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutOrderAdminApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired AddressRepository addresses;
    @Autowired ShippingRangeRepository shippingRanges;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired OrderAddressRepository orderAddresses;
    @MockitoSpyBean SimulatedPaymentRepository simulatedPayments;
    @Autowired PasswordEncoder encoder;
    @Autowired CheckoutService checkoutService;
    @Autowired AdminProvisioningService adminProvisioning;
    @Autowired SecurityConfig.LoginAttemptGuard loginAttemptGuard;

    Product productA;
    Product productB;
    User customer;
    User otherCustomer;
    User admin;

    @BeforeEach
    void setup() {
        reset(simulatedPayments);
        loginAttemptGuard.clearAll();
        simulatedPayments.deleteAll();
        orderAddresses.deleteAll();
        orderItems.deleteAll();
        orders.deleteAll();
        addresses.deleteAll();
        shippingRanges.deleteAll();
        products.deleteAll();
        users.deleteAll();

        new DemoShippingRangeLoader(shippingRanges).run();

        productA = products.saveAndFlush(new Product(
                "Cupcake Baunilha Teste",
                "Descrição baunilha teste",
                new BigDecimal("12.50"),
                "/assets/img/cupcake-baunilha.svg",
                true
        ));
        productB = products.saveAndFlush(new Product(
                "Cupcake Chocolate Teste",
                "Descrição chocolate teste",
                new BigDecimal("14.00"),
                "/assets/img/cupcake-chocolate.svg",
                true
        ));

        customer = users.saveAndFlush(new User(
                "Cliente Principal",
                "cliente@example.com",
                encoder.encode("senha-cliente-123"),
                Role.CLIENTE
        ));
        otherCustomer = users.saveAndFlush(new User(
                "Outro Cliente",
                "outro@example.com",
                encoder.encode("senha-outro-123"),
                Role.CLIENTE
        ));
        admin = users.saveAndFlush(new User(
                "Admin Loja",
                "admin@example.com",
                encoder.encode("senha-admin-123"),
                Role.ADMIN
        ));
    }

    private void addItemToCart(MockHttpSession session, long productId, int quantity) throws Exception {
        mvc.perform(post("/api/carrinho/itens")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"produtoId\":" + productId + ",\"quantidade\":" + quantity + "}"))
                .andExpect(status().isOk());
    }

    private JsonNode createAddress( String email, String cep, String uf) throws Exception {
        String payload = mapper.writeValueAsString(Map.of(
                "destinatario", "Destinatário Teste",
                "cep", cep,
                "logradouro", "Quadra Teste 10",
                "numero", "100",
                "complemento", "Apto 202",
                "bairro", "Asa Sul",
                "cidade", "Brasília",
                "uf", uf
        ));
        String body = mvc.perform(post("/api/conta/enderecos")
                        .with(user(email).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    // 1. Compra de retirada e entrega com valores da faixa, dados proprios e registro completo
    @Test
    void pickupAndDeliveryOrdersCalculateTotalsAndPersistFullRecord() throws Exception {
        MockHttpSession session = new MockHttpSession();
        addItemToCart(session, productA.getId(), 2); // 2 x 12.50 = 25.00
        addItemToCart(session, productB.getId(), 1); // 1 x 14.00 = 14.00 -> subtotal 39.00

        // Revisao de retirada (frete 0.00)
        String revPickupBody = mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.modalidade").value("RETIRADA"))
                .andExpect(jsonPath("$.endereco").doesNotExist())
                .andExpect(jsonPath("$.subtotal").value("39.00"))
                .andExpect(jsonPath("$.frete").value("0.00"))
                .andExpect(jsonPath("$.total").value("39.00"))
                .andReturn().getResponse().getContentAsString();
        String pickupRevId = mapper.readTree(revPickupBody).get("revisaoId").asText();

        String pickupOrderBody = mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + pickupRevId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidade").value("RETIRADA"))
                .andExpect(jsonPath("$.statusAtendimento").value("RECEBIDO"))
                .andExpect(jsonPath("$.resultadoSimulado").value("APROVADO"))
                .andExpect(jsonPath("$.endereco").doesNotExist())
                .andExpect(jsonPath("$.subtotal").value("39.00"))
                .andExpect(jsonPath("$.frete").value("0.00"))
                .andExpect(jsonPath("$.total").value("39.00"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long pickupOrderId = mapper.readTree(pickupOrderBody).get("id").asLong();
        assertThat(orderAddresses.findByOrderId(pickupOrderId)).isEmpty();

        // Carrinho foi limpo apos o commit do pedido aprovado
        mvc.perform(get("/api/carrinho").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vazio").value(true));

        // Compra com entrega nas duas faixas de demonstracao (70000-001 -> R$ 8.00 e 71000-001 -> R$ 12.00)
        JsonNode addrFaixa1 = createAddress(customer.getNormalizedEmail(), "70000-001", "DF");
        JsonNode addrFaixa2 = createAddress(customer.getNormalizedEmail(), "71000-001", "DF");

        addItemToCart(session, productA.getId(), 2); // 25.00
        String revDel1 = mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + addrFaixa1.get("id").asLong()
                                + ",\"versaoEndereco\":" + addrFaixa1.get("versao").asLong() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value("25.00"))
                .andExpect(jsonPath("$.frete").value("8.00"))
                .andExpect(jsonPath("$.total").value("33.00"))
                .andExpect(jsonPath("$.endereco.cep").value("70000001"))
                .andReturn().getResponse().getContentAsString();

        String revDel2 = mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + addrFaixa2.get("id").asLong()
                                + ",\"versaoEndereco\":" + addrFaixa2.get("versao").asLong() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value("25.00"))
                .andExpect(jsonPath("$.frete").value("12.00"))
                .andExpect(jsonPath("$.total").value("37.00"))
                .andReturn().getResponse().getContentAsString();
        String delRevId = mapper.readTree(revDel2).get("revisaoId").asText();

        String delOrderBody = mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + delRevId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidade").value("ENTREGA"))
                .andExpect(jsonPath("$.statusAtendimento").value("RECEBIDO"))
                .andExpect(jsonPath("$.resultadoSimulado").value("APROVADO"))
                .andExpect(jsonPath("$.subtotal").value("25.00"))
                .andExpect(jsonPath("$.frete").value("12.00"))
                .andExpect(jsonPath("$.total").value("37.00"))
                .andExpect(jsonPath("$.endereco.cep").value("71000001"))
                .andReturn().getResponse().getContentAsString();
        long delOrderId = mapper.readTree(delOrderBody).get("id").asLong();
        assertThat(orderAddresses.findByOrderId(delOrderId)).isPresent();
    }

    // 2. Recusa simulada persistida, carrinho mantido e atendimento impedido
    @Test
    void refusedSimulatedPaymentPersistsOrderKeepsCartAndBlocksAttendance() throws Exception {
        MockHttpSession session = new MockHttpSession();
        addItemToCart(session, productA.getId(), 3); // 37.50

        String revBody = mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String revId = mapper.readTree(revBody).get("revisaoId").asText();

        String refusedBody = mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"RECUSADO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultadoSimulado").value("RECUSADO"))
                .andExpect(jsonPath("$.statusAtendimento").value("NAO_AUTORIZADO"))
                .andReturn().getResponse().getContentAsString();
        JsonNode refusedOrder = mapper.readTree(refusedBody);
        long refusedOrderId = refusedOrder.get("id").asLong();
        long refusedVersion = refusedOrder.get("versao").asLong();

        // Carrinho permanece intacto para nova revisao
        mvc.perform(get("/api/carrinho").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vazio").value(false))
                .andExpect(jsonPath("$.quantidadeTotal").value(3));

        // Atendimento impedido para pedido recusado
        for (String targetStatus : new String[]{"EM_PREPARACAO", "PRONTO_PARA_RETIRADA", "CONCLUIDO", "CANCELADO"}) {
            mvc.perform(put("/api/admin/pedidos/" + refusedOrderId + "/status")
                            .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                            .with(csrf())
                            .contentType("application/json")
                            .content("{\"statusAtendimento\":\"" + targetStatus + "\",\"versao\":" + refusedVersion + "}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("TRANSICAO_STATUS_INVALIDA"));
        }

        // Nova tentativa usa nova revisao e cria outro pedido aprovado
        String nextRevBody = mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String nextRevId = mapper.readTree(nextRevBody).get("revisaoId").asText();
        assertThat(nextRevId).isNotEqualTo(revId);

        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + nextRevId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusAtendimento").value("RECEBIDO"));

        mvc.perform(get("/api/carrinho").session(session))
                .andExpect(jsonPath("$.vazio").value(true));
    }

    // 3. Vazio, itens bloqueados, endereco alheio, revisao expirada e campos adulterados rejeitados
    @Test
    void rejectsEmptyCartBlockedItemsForeignAddressExpiredRevisionAndTamperedFields() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Carrinho vazio
        mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CARRINHO_VAZIO"));

        // Item bloqueado no carrinho
        addItemToCart(session, productA.getId(), 1);
        productA.setAvailable(false);
        productA = products.saveAndFlush(productA);
        mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ITEM_BLOQUEADO"));

        productA.setAvailable(true);
        productA = products.saveAndFlush(productA);

        // Endereco alheio retorna 404 sem revelar dados
        JsonNode foreignAddress = createAddress(otherCustomer.getNormalizedEmail(), "70000-001", "DF");
        mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + foreignAddress.get("id").asLong()
                                + ",\"versaoEndereco\":" + foreignAddress.get("versao").asLong() + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ENDERECO_NAO_ENCONTRADO"));

        // Endereco fora da faixa ativa
        JsonNode outsideAddress = createAddress(customer.getNormalizedEmail(), "72000-001", "DF");
        mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + outsideAddress.get("id").asLong()
                                + ",\"versaoEndereco\":" + outsideAddress.get("versao").asLong() + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("FORA_DA_AREA_DE_ENTREGA"));

        // Campos adulterados / extras rejeitados com 400
        for (String invalidRevisionJson : new String[]{
                "{\"modalidade\":\"RETIRADA\",\"total\":\"0.01\"}",
                "{\"modalidade\":\"RETIRADA\",\"usuarioId\":999}",
                "{\"modalidade\":\"RETIRADA\",\"enderecoId\":1}",
                "{\"modalidade\":\"ENTREGA\"}"
        }) {
            mvc.perform(post("/api/conta/checkout/revisao")
                            .session(session)
                            .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                            .with(csrf())
                            .contentType("application/json")
                            .content(invalidRevisionJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
        }

        // Revisao expirada rejeitada
        String revBody = mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String revId = mapper.readTree(revBody).get("revisaoId").asText();

        // Campos extras na confirmacao rejeitados
        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"APROVADO\",\"total\":\"0.01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));

        // Outro usuario ou outra sessao nao pode usar a revisao
        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(otherCustomer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REVISAO_NAO_ENCONTRADA"));

        checkoutService.expireRevisionForTest(session, revId);
        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REVISAO_EXPIRADA"));

        assertThat(orders.count()).isZero();
    }

    // 4. Mudanca de preco, disponibilidade, endereco e frete entre revisao/confirmacao rejeitada sem pedido parcial
    @Test
    void rejectsChangesInPriceAvailabilityAddressAndShippingBetweenRevisionAndConfirmation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        addItemToCart(session, productA.getId(), 1);
        JsonNode address = createAddress(customer.getNormalizedEmail(), "70000-001", "DF");

        // 4a. Mudanca de preco
        String revPrice = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        productA.setPrice(new BigDecimal("19.90"));
        productA = products.saveAndFlush(productA);

        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revPrice + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REVISAO_DESATUALIZADA"));

        // 4b. Mudanca de disponibilidade
        String revAvail = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        productA.setAvailable(false);
        productA = products.saveAndFlush(productA);

        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revAvail + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REVISAO_DESATUALIZADA"));

        productA.setAvailable(true);
        productA = products.saveAndFlush(productA);

        // 4c. Mudanca de endereco
        String revAddr = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + address.get("id").asLong()
                                + ",\"versaoEndereco\":" + address.get("versao").asLong() + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        String updatedAddressBody = mvc.perform(put("/api/conta/enderecos/" + address.get("id").asLong())
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "destinatario", "Destinatário Editado",
                                "cep", "70000-001",
                                "logradouro", "Quadra Editada",
                                "numero", "200",
                                "bairro", "Asa Sul",
                                "cidade", "Brasília",
                                "uf", "DF",
                                "versao", address.get("versao").asLong()
                        ))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode updatedAddr = mapper.readTree(updatedAddressBody);

        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revAddr + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REVISAO_DESATUALIZADA"));

        // 4d. Mudanca de faixa de frete
        String revShip = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + updatedAddr.get("id").asLong()
                                + ",\"versaoEndereco\":" + updatedAddr.get("versao").asLong() + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        shippingRanges.deleteAll();
        shippingRanges.saveAndFlush(new ShippingRange("70000000", "70999999", new BigDecimal("15.00"), true));

        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revShip + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REVISAO_DESATUALIZADA"));

        assertThat(orders.count()).isZero();
    }

    // 5. Repeticao, duas confirmacoes concorrentes e resposta perdida gerando apenas um pedido
    @Test
    void idempotentRetryLostResponseConcurrentConfirmationsAndDivergentResult() throws Exception {
        MockHttpSession session = new MockHttpSession();
        addItemToCart(session, productA.getId(), 2);

        String revId = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        // Duas confirmacoes concorrentes com a mesma chave revisaoId
        var startLatch = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstFuture = executor.submit(() -> {
                startLatch.await(2, TimeUnit.SECONDS);
                return mvc.perform(post("/api/conta/pedidos")
                                .session(session)
                                .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                        .andReturn().getResponse();
            });
            var secondFuture = executor.submit(() -> {
                startLatch.await(2, TimeUnit.SECONDS);
                return mvc.perform(post("/api/conta/pedidos")
                                .session(session)
                                .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                        .andReturn().getResponse();
            });
            startLatch.countDown();
            var r1 = firstFuture.get(5, TimeUnit.SECONDS);
            var r2 = secondFuture.get(5, TimeUnit.SECONDS);
            assertThat(java.util.List.of(r1.getStatus(), r2.getStatus())).containsExactlyInAnyOrder(201, 200);
            long id1 = mapper.readTree(r1.getContentAsString()).get("id").asLong();
            long id2 = mapper.readTree(r2.getContentAsString()).get("id").asLong();
            assertThat(id1).isEqualTo(id2);
        }

        assertThat(orders.count()).isEqualTo(1L);

        // Recuperacao de resposta perdida mesmo apos o carrinho ter sido esvaziado retorna 200 com o mesmo pedido
        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisaoId").value(revId));

        // Tentativa de reenviar a mesma chave com resultado divergente retorna 409 CONFIRMACAO_DIVERGENTE
        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"RECUSADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFIRMACAO_DIVERGENTE"));
    }

    // 6. Erro de persistencia com rollback e carrinho nao limpo antes do commit
    @Test
    void persistenceFailureRollsBackTransactionAndDoesNotClearCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        addItemToCart(session, productA.getId(), 2);

        String revId = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        doThrow(new RuntimeException("Falha simulada de persistencia no pagamento"))
                .when(simulatedPayments).save(any());

        mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        reset(simulatedPayments);

        // Rollback completo: nenhum pedido ou item gravado, e carrinho permanece intacto
        assertThat(orders.count()).isZero();
        assertThat(orderItems.count()).isZero();
        mvc.perform(get("/api/carrinho").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vazio").value(false))
                .andExpect(jsonPath("$.quantidadeTotal").value(2));
    }

    // 7. Historico preservado apos edicao do produto/endereco e acesso a pedido alheio bloqueado
    @Test
    void preservesHistoricalItemAndAddressDataAndBlocksForeignOrderAccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        addItemToCart(session, productA.getId(), 2);
        JsonNode address = createAddress(customer.getNormalizedEmail(), "70000-001", "DF");

        String revId = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + address.get("id").asLong()
                                + ",\"versaoEndereco\":" + address.get("versao").asLong() + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        String orderBody = mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + revId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = mapper.readTree(orderBody).get("id").asLong();

        // Admin edita nome e preco do produto no catalogo
        mvc.perform(put("/api/admin/produtos/" + productA.getId())
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "nome", "Nome Novo Pós-Compra",
                                "descricao", "Descrição alterada",
                                "preco", "99.90",
                                "disponivel", false,
                                "versao", productA.getVersion()
                        ))))
                .andExpect(status().isOk());

        // Cliente edita e depois remove o endereco do cadastro
        mvc.perform(delete("/api/conta/enderecos/" + address.get("id").asLong())
                        .param("versao", String.valueOf(address.get("versao").asLong()))
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Pedido preserva nome, preco e endereco historicos da compra
        mvc.perform(get("/api/conta/pedidos/" + orderId)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].nome").value("Cupcake Baunilha Teste"))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value("12.50"))
                .andExpect(jsonPath("$.itens[0].subtotal").value("25.00"))
                .andExpect(jsonPath("$.endereco.destinatario").value("Destinatário Teste"))
                .andExpect(jsonPath("$.endereco.logradouro").value("Quadra Teste 10"))
                .andExpect(jsonPath("$.total").value("33.00"));

        // Outro cliente recebe 404 ao tentar consultar pedido alheio
        mvc.perform(get("/api/conta/pedidos/" + orderId)
                        .with(user(otherCustomer.getNormalizedEmail()).roles("CLIENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_NAO_ENCONTRADO"));

        mvc.perform(get("/api/conta/pedidos")
                        .with(user(otherCustomer.getNormalizedEmail()).roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // 8. Cadastro/edicao de produto por ADMIN; cliente e visitante sem privilegios; e-mail existente nao promovido
    @Test
    void adminProductCrudRoleProtectionAndSafeAdminProvisioning() throws Exception {
        // Visitante recebe 401 e CLIENTE recebe 403 nas rotas administrativas
        mvc.perform(get("/api/admin/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/produtos").with(user(customer.getNormalizedEmail()).roles("CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/produtos").with(user(customer.getNormalizedEmail()).roles("CLIENTE")).with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());

        // Criacao valida por ADMIN
        String createdBody = mvc.perform(post("/api/admin/produtos")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "nome", "Cupcake Frutas Vermelhas Admin",
                                "descricao", "Cobertura artesanal de frutas vermelhas",
                                "preco", "16.75",
                                "disponivel", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Cupcake Frutas Vermelhas Admin"))
                .andExpect(jsonPath("$.preco").value("16.75"))
                .andExpect(jsonPath("$.versao").value(0))
                .andReturn().getResponse().getContentAsString();
        long createdId = mapper.readTree(createdBody).get("id").asLong();

        // Entradas invalidas (preco negativo, 3 casas decimais, URL externa de imagem, id/versao no POST) rejeitadas
        for (String invalidJson : new String[]{
                "{\"nome\":\"Teste\",\"descricao\":\"Desc\",\"preco\":\"-1.00\",\"disponivel\":true}",
                "{\"nome\":\"Teste\",\"descricao\":\"Desc\",\"preco\":\"12.345\",\"disponivel\":true}",
                "{\"nome\":\"Teste\",\"descricao\":\"Desc\",\"preco\":\"12.00\",\"imagemCaminho\":\"https://example.com/a.svg\",\"disponivel\":true}",
                "{\"id\":99,\"nome\":\"Teste\",\"descricao\":\"Desc\",\"preco\":\"12.00\",\"disponivel\":true}",
                "{\"versao\":0,\"nome\":\"Teste\",\"descricao\":\"Desc\",\"preco\":\"12.00\",\"disponivel\":true}"
        }) {
            mvc.perform(post("/api/admin/produtos")
                            .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                            .with(csrf())
                            .contentType("application/json")
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
        }

        // Edicao com versao atual funciona; versao antiga retorna 409 PRODUTO_DESATUALIZADO
        mvc.perform(put("/api/admin/produtos/" + createdId)
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "nome", "Cupcake Frutas Vermelhas Editado",
                                "descricao", "Descrição atualizada",
                                "preco", "17.50",
                                "imagemCaminho", "/assets/img/cupcake-caramelo.svg",
                                "disponivel", false,
                                "versao", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.disponivel").value(false));

        mvc.perform(put("/api/admin/produtos/" + createdId)
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of(
                                "nome", "Conflito",
                                "descricao", "Conflito",
                                "preco", "18.00",
                                "disponivel", true,
                                "versao", 0
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRODUTO_DESATUALIZADO"));

        // Provisionamento seguro: nao promove CLIENTE existente e nao sobrescreve senha de ADMIN existente
        assertThatThrownBy(() -> adminProvisioning.provisionAdmin("Tentativa Promoção", customer.getNormalizedEmail(), "nova-senha-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLIENTE");
        assertThat(users.findByNormalizedEmail(customer.getNormalizedEmail()).orElseThrow().getRole()).isEqualTo(Role.CLIENTE);

        assertThatThrownBy(() -> adminProvisioning.provisionAdmin("Admin Loja", admin.getNormalizedEmail(), "outra-senha-999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN");

        // Provisionamento idempotente para o mesmo ADMIN nao altera o registro
        var idempotentAdmin = adminProvisioning.provisionAdmin("Admin Loja", admin.getNormalizedEmail(), "senha-admin-123");
        assertThat(idempotentAdmin.id()).isEqualTo(admin.getId());

        // Protecao contra abuso de login bloqueia com 429 apos tentativas consecutivas invalidas
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/autenticacao/login")
                            .with(csrf())
                            .contentType("application/x-www-form-urlencoded")
                            .param("email", "abuso@exemplo.com")
                            .param("senha", "errada"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
        }
        mvc.perform(post("/api/autenticacao/login")
                        .with(csrf())
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "abuso@exemplo.com")
                        .param("senha", "senha-qualquer-123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("MUITAS_TENTATIVAS_LOGIN"));
    }

    // 9. Atendimento por modalidade, versao, cancelamento e estados finais respeitados
    @Test
    void orderAttendanceWorkflowRespectsModalityVersionCancellationAndFinalStates() throws Exception {
        MockHttpSession session = new MockHttpSession();
        JsonNode address = createAddress(customer.getNormalizedEmail(), "70000-001", "DF");

        // Pedido de ENTREGA aprovado
        addItemToCart(session, productA.getId(), 1);
        String delRevId = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":" + address.get("id").asLong()
                                + ",\"versaoEndereco\":" + address.get("versao").asLong() + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        JsonNode delOrder = mapper.readTree(mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + delRevId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long delId = delOrder.get("id").asLong();

        // Salto RECEBIDO -> CONCLUIDO rejeitado
        mvc.perform(put("/api/admin/pedidos/" + delId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"CONCLUIDO\",\"versao\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_STATUS_INVALIDA"));

        // RECEBIDO -> EM_PREPARACAO
        mvc.perform(put("/api/admin/pedidos/" + delId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"EM_PREPARACAO\",\"versao\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusAtendimento").value("EM_PREPARACAO"))
                .andExpect(jsonPath("$.versao").value(1));

        // Ramo errado da modalidade (ENTREGA -> PRONTO_PARA_RETIRADA) rejeitado
        mvc.perform(put("/api/admin/pedidos/" + delId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"PRONTO_PARA_RETIRADA\",\"versao\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_STATUS_INVALIDA"));

        // Repetir mesmo estado e idempotente e nao incrementa versao
        mvc.perform(put("/api/admin/pedidos/" + delId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"EM_PREPARACAO\",\"versao\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1));

        // EM_PREPARACAO -> SAIU_PARA_ENTREGA
        mvc.perform(put("/api/admin/pedidos/" + delId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"SAIU_PARA_ENTREGA\",\"versao\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusAtendimento").value("SAIU_PARA_ENTREGA"))
                .andExpect(jsonPath("$.versao").value(2));

        // Cancelamento apos saida para entrega e proibido
        mvc.perform(put("/api/admin/pedidos/" + delId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"CANCELADO\",\"versao\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_STATUS_INVALIDA"));

        // SAIU_PARA_ENTREGA -> CONCLUIDO
        mvc.perform(put("/api/admin/pedidos/" + delId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"CONCLUIDO\",\"versao\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusAtendimento").value("CONCLUIDO"))
                .andExpect(jsonPath("$.versao").value(3));

        // Pedido de RETIRADA aprovado: RECEBIDO -> EM_PREPARACAO -> PRONTO_PARA_RETIRADA -> CANCELADO
        addItemToCart(session, productB.getId(), 1);
        String pickRevId = mapper.readTree(mvc.perform(post("/api/conta/checkout/revisao")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("revisaoId").asText();

        long pickId = mapper.readTree(mvc.perform(post("/api/conta/pedidos")
                        .session(session)
                        .with(user(customer.getNormalizedEmail()).roles("CLIENTE"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"revisaoId\":\"" + pickRevId + "\",\"resultadoSimulado\":\"APROVADO\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/admin/pedidos/" + pickId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"EM_PREPARACAO\",\"versao\":0}"))
                .andExpect(status().isOk());

        mvc.perform(put("/api/admin/pedidos/" + pickId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"PRONTO_PARA_RETIRADA\",\"versao\":1}"))
                .andExpect(status().isOk());

        // Conflito de versao desatualizada retorna 409 PEDIDO_DESATUALIZADO
        mvc.perform(put("/api/admin/pedidos/" + pickId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"CONCLUIDO\",\"versao\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_DESATUALIZADO"));

        // Cancelamento permitido a partir de PRONTO_PARA_RETIRADA
        mvc.perform(put("/api/admin/pedidos/" + pickId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"CANCELADO\",\"versao\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusAtendimento").value("CANCELADO"));

        // Estado final CANCELADO nao aceita novas transicoes
        mvc.perform(put("/api/admin/pedidos/" + pickId + "/status")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"statusAtendimento\":\"CONCLUIDO\",\"versao\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_STATUS_INVALIDA"));

        // Cliente acompanha estados atualizados na sua conta e listagem admin nunca expoe hash de senha
        String adminOrdersJson = mvc.perform(get("/api/admin/pedidos")
                        .with(user(admin.getNormalizedEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(adminOrdersJson).doesNotContain("bcrypt", "senhaHash", "passwordHash");
    }
}
