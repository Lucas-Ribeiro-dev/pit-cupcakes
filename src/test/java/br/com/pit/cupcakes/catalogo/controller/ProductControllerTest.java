package br.com.pit.cupcakes.catalogo.controller;

import br.com.pit.cupcakes.catalogo.dto.ProductResponse;
import br.com.pit.cupcakes.catalogo.service.ProductService;
import br.com.pit.cupcakes.compartilhado.exception.GlobalExceptionHandler;
import br.com.pit.cupcakes.compartilhado.exception.InvalidIdException;
import br.com.pit.cupcakes.compartilhado.exception.ProductNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({GlobalExceptionHandler.class, br.com.pit.cupcakes.config.SecurityConfig.class,
        br.com.pit.cupcakes.identidade.service.UserAuthenticationService.class})
class ProductControllerTest {

    @MockitoBean
    private br.com.pit.cupcakes.identidade.repository.UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("TC01 - Consultar catálogo populado deve retornar status 200 e campos públicos")
    void shouldReturnPopulatedCatalog() throws Exception {
        List<ProductResponse> products = List.of(
                new ProductResponse(1L, "Cupcake de Baunilha", "Delicioso cupcake", "12.50", "/assets/img/baunilha.svg", true),
                new ProductResponse(2L, "Cupcake de Chocolate", "Massa de cacau", "14.00", "/assets/img/chocolate.svg", true)
        );

        when(productService.findAll()).thenReturn(products);

        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].nome", is("Cupcake de Baunilha")))
                .andExpect(jsonPath("$[0].descricao", is("Delicioso cupcake")))
                .andExpect(jsonPath("$[0].preco", is("12.50")))
                .andExpect(jsonPath("$[0].imagemUrl", is("/assets/img/baunilha.svg")))
                .andExpect(jsonPath("$[0].disponivel", is(true)))
                .andExpect(jsonPath("$[0].versao").doesNotExist())
                .andExpect(jsonPath("$[0].version").doesNotExist());
    }

    @Test
    @DisplayName("TC02 - Consultar catálogo sem produtos deve retornar status 200 com array vazio")
    void shouldReturnEmptyCatalog() throws Exception {
        when(productService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("TC03 - Consultar ID existente deve retornar 200 e produto correspondente")
    void shouldReturnProductWhenIdExists() throws Exception {
        ProductResponse product = new ProductResponse(1L, "Cupcake de Baunilha", "Delicioso cupcake", "12.50", null, true);

        when(productService.findById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/produtos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nome", is("Cupcake de Baunilha")))
                .andExpect(jsonPath("$.preco", is("12.50")))
                .andExpect(jsonPath("$.imagemUrl", nullValue()))
                .andExpect(jsonPath("$.disponivel", is(true)));
    }

    @Test
    @DisplayName("TC04 - Consultar ID inexistente deve retornar 404 e código PRODUTO_NAO_ENCONTRADO")
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.findById(999L))
                .thenThrow(new ProductNotFoundException("Produto não encontrado para o identificador informado."));

        mockMvc.perform(get("/api/produtos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is("PRODUTO_NAO_ENCONTRADO")))
                .andExpect(jsonPath("$.mensagem", is("Produto não encontrado para o identificador informado.")));
    }

    @Test
    @DisplayName("TC05 - Consultar ID zero ou negativo deve retornar 400 e código ID_INVALIDO")
    void shouldReturn400WhenIdIsZeroOrNegative() throws Exception {
        when(productService.findById(0L))
                .thenThrow(new InvalidIdException("O identificador informado deve ser um número inteiro positivo."));

        mockMvc.perform(get("/api/produtos/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("ID_INVALIDO")))
                .andExpect(jsonPath("$.mensagem", is("O identificador informado deve ser um número inteiro positivo.")));
    }

    @Test
    @DisplayName("TC05 - Consultar ID alfanumérico deve retornar 400 e código ID_INVALIDO")
    void shouldReturn400WhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/produtos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("ID_INVALIDO")))
                .andExpect(jsonPath("$.mensagem", is("O identificador informado deve ser um número inteiro positivo.")));
    }

    @Test
    @DisplayName("TC07 - Falha interna deve retornar 500 com código ERRO_INTERNO e sem stack trace")
    void shouldReturn500OnInternalError() throws Exception {
        when(productService.findAll()).thenThrow(new RuntimeException("Simulando falha inesperada"));

        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo", is("ERRO_INTERNO")))
                .andExpect(jsonPath("$.mensagem", is("Ocorreu um erro interno no servidor.")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}

