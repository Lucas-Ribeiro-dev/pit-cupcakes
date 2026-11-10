package br.com.pit.cupcakes.carrinho;

import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.carrinho.service.CartService;
import br.com.pit.cupcakes.carrinho.service.CartOperationException;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartApiTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired CartService cartService;
    Product product;
    MockHttpSession session;

    @BeforeEach void setup() {
        products.deleteAll();
        product = products.save(new Product("Cupcake teste", "Teste", new BigDecimal("12.34"), null, true));
        session = new MockHttpSession();
    }

    private String add(long id, int quantity) {
        return "{\"produtoId\":" + id + ",\"quantidade\":" + quantity + "}";
    }

    @Test void emptyAddRepeatedChangeAndRemove() throws Exception {
        mvc.perform(get("/api/carrinho").session(session)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.vazio").value(true)).andExpect(jsonPath("$.subtotal").value("0.00"));
        for (int quantity : new int[]{2, 3}) mvc.perform(post("/api/carrinho/itens").session(session).with(csrf())
                .contentType("application/json").content(add(product.getId(), quantity))).andExpect(status().isOk());
        mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.quantidadeTotal").value(5)).andExpect(jsonPath("$.subtotal").value("61.70"));
        mvc.perform(put("/api/carrinho/itens/" + product.getId()).session(session).with(csrf())
                .contentType("application/json").content("{\"quantidade\":4}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value("49.36"));
        for (int i = 0; i < 2; i++) mvc.perform(delete("/api/carrinho/itens/" + product.getId()).session(session).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.vazio").value(true));
    }

    @Test void rejectsInvalidInputAndKeepsPreviousState() throws Exception {
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content(add(product.getId(), 1))).andExpect(status().isOk());
        for (String body : new String[]{"{\"produtoId\":"+product.getId()+",\"quantidade\":0}",
                "{\"produtoId\":"+product.getId()+",\"quantidade\":1.5}",
                "{\"produtoId\":"+product.getId()+",\"quantidade\":100}"})
            mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("QUANTIDADE_INVALIDA"));
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content("{\"produtoId\":"+product.getId()+",\"quantidade\":1,\"preco\":\"0.01\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content(add(0, 1))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("ID_INVALIDO"));
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content(add(product.getId(), 99))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/carrinho/itens/0").session(session).with(csrf()).contentType("application/json")
                .content("{\"quantidade\":2}")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/carrinho/itens/abc").session(session).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.subtotal").value("12.34"));
    }

    @Test void blocksUnavailableAndRemovedItemsAndRefreshesPrice() throws Exception {
        long id = product.getId();
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content(add(id, 2))).andExpect(status().isOk());
        product.setPrice(new BigDecimal("15.00")); product = products.saveAndFlush(product);
        mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.subtotal").value("30.00"));
        product.setAvailable(false); product = products.saveAndFlush(product);
        mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.possuiItensBloqueados").value(true))
                .andExpect(jsonPath("$.itens[0].motivoBloqueio").value("PRODUTO_INDISPONIVEL"))
                .andExpect(jsonPath("$.itens[0].subtotal").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.subtotal").value("0.00"));
        mvc.perform(put("/api/carrinho/itens/"+id).session(session).with(csrf()).contentType("application/json")
                .content("{\"quantidade\":3}")).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ITEM_BLOQUEADO"));
        products.delete(product);
        mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.itens[0].nome").value("Produto não encontrado"))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(delete("/api/carrinho/itens/"+id).session(session).with(csrf())).andExpect(status().isNoContent());
    }

    @Test void isolatesSessionsAndPreservesSecurity() throws Exception {
        mvc.perform(post("/api/carrinho/itens").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content(add(product.getId(), 1))).andExpect(status().isOk());
        mvc.perform(get("/api/carrinho").session(new MockHttpSession())).andExpect(jsonPath("$.vazio").value(true));
        mvc.perform(get("/api/conta/enderecos").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/qualquer").session(session)).andExpect(status().isUnauthorized());
    }

    @Test void rejectsMissingUnavailableAndExcessLines() throws Exception {
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content(add(999999999L, 1))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PRODUTO_NAO_ENCONTRADO"));
        mvc.perform(put("/api/carrinho/itens/"+product.getId()).session(session).with(csrf())
                .contentType("application/json").content("{\"quantidade\":1}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("ITEM_NAO_ENCONTRADO"));
        product.setAvailable(false); product = products.saveAndFlush(product);
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content(add(product.getId(), 1))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRODUTO_INDISPONIVEL"));
        var fixtures = new java.util.ArrayList<Product>();
        for (int i = 0; i < 51; i++) fixtures.add(new Product("Fixture " + i, "Teste", new BigDecimal("1.00"), null, true));
        var saved = products.saveAll(fixtures);
        for (int i = 0; i < 50; i++) cartService.add(session, saved.get(i).getId(), 1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> cartService.add(session, saved.get(50).getId(), 1))
                .isInstanceOf(CartOperationException.class)
                .hasMessageContaining("50 produtos");
        mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.itens.length()").value(50));
    }

    @Test void concurrentAddsKeepBothUpdates() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> mvc.perform(post("/api/carrinho/itens").session(session).with(csrf())
                    .contentType("application/json").content(add(product.getId(), 2))).andReturn());
            var second = executor.submit(() -> mvc.perform(post("/api/carrinho/itens").session(session).with(csrf())
                    .contentType("application/json").content(add(product.getId(), 3))).andReturn());
            assertThat(first.get().getResponse().getStatus()).isEqualTo(200);
            assertThat(second.get().getResponse().getStatus()).isEqualTo(200);
        }
        mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.quantidadeTotal").value(5));
    }

    @Test void keepsCartThroughLoginAndClearsItOnLogout() throws Exception {
        String email = java.util.UUID.randomUUID() + "@example.com";
        String password = "senha fictícia";
        users.save(new User("Cliente", email, encoder.encode(password)));
        mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                .content(add(product.getId(), 2))).andExpect(status().isOk());
        var csrfResponse = mvc.perform(get("/api/autenticacao/csrf").session(session)).andReturn();
        var token = mapper.readTree(csrfResponse.getResponse().getContentAsString());
        String oldId = session.getId();
        var login = mvc.perform(post("/api/autenticacao/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType("application/x-www-form-urlencoded").param("email", email).param("senha", password))
                .andExpect(status().isOk()).andReturn();
        var authenticated = (MockHttpSession) login.getRequest().getSession();
        assertThat(authenticated.getId()).isNotEqualTo(oldId);
        mvc.perform(get("/api/carrinho").session(authenticated)).andExpect(jsonPath("$.quantidadeTotal").value(2));
        var renewed = mapper.readTree(mvc.perform(get("/api/autenticacao/csrf").session(authenticated))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/autenticacao/logout").session(authenticated)
                .header(renewed.get("headerName").asText(), renewed.get("token").asText()))
                .andExpect(status().isNoContent());
        assertThat(authenticated.isInvalid()).isTrue();
        var nextSession = new MockHttpSession();
        var nextToken = mapper.readTree(mvc.perform(get("/api/autenticacao/csrf").session(nextSession))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/autenticacao/login").session(nextSession)
                .header(nextToken.get("headerName").asText(), nextToken.get("token").asText())
                .contentType("application/x-www-form-urlencoded").param("email", email).param("senha", password))
                .andExpect(status().isOk());
        mvc.perform(get("/api/carrinho").session(nextSession)).andExpect(jsonPath("$.vazio").value(true));
    }
}
