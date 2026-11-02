package br.com.pit.cupcakes.catalogo.service;

import br.com.pit.cupcakes.catalogo.dto.ProductResponse;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.compartilhado.exception.InvalidIdException;
import br.com.pit.cupcakes.compartilhado.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    @DisplayName("Deve retornar todos os produtos ordenados por id crescente")
    void shouldReturnAllProductsOrderedById() {
        Product p1 = new Product("Baunilha", "Delicioso", new BigDecimal("12.50"), "/img/1.svg", true);
        p1.setId(1L);
        Product p2 = new Product("Chocolate", "Gourmet", new BigDecimal("14.00"), "/img/2.svg", false);
        p2.setId(2L);

        when(productRepository.findAllByOrderByIdAsc()).thenReturn(List.of(p1, p2));

        List<ProductResponse> responses = productService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).name()).isEqualTo("Baunilha");
        assertThat(responses.get(0).price()).isEqualTo("12.50");
        assertThat(responses.get(0).available()).isTrue();

        assertThat(responses.get(1).id()).isEqualTo(2L);
        assertThat(responses.get(1).name()).isEqualTo("Chocolate");
        assertThat(responses.get(1).price()).isEqualTo("14.00");
        assertThat(responses.get(1).available()).isFalse();

        verify(productRepository).findAllByOrderByIdAsc();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o repositório estiver vazio")
    void shouldReturnEmptyListWhenNoProductsExist() {
        when(productRepository.findAllByOrderByIdAsc()).thenReturn(Collections.emptyList());

        List<ProductResponse> responses = productService.findAll();

        assertThat(responses).isEmpty();
        verify(productRepository).findAllByOrderByIdAsc();
    }

    @Test
    @DisplayName("Deve retornar o produto correto por id")
    void shouldReturnProductById() {
        Product product = new Product("Morango", "Fresco", new BigDecimal("15.50"), null, true);
        product.setId(3L);

        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("Morango");
        assertThat(response.imageUrl()).isNull();
        assertThat(response.price()).isEqualTo("15.50");
    }

    @Test
    @DisplayName("Deve lançar InvalidIdException para id nulo, zero ou negativo")
    void shouldThrowInvalidIdException() {
        assertThatThrownBy(() -> productService.findById(null))
                .isInstanceOf(InvalidIdException.class)
                .hasMessageContaining("número inteiro positivo");

        assertThatThrownBy(() -> productService.findById(0L))
                .isInstanceOf(InvalidIdException.class)
                .hasMessageContaining("número inteiro positivo");

        assertThatThrownBy(() -> productService.findById(-5L))
                .isInstanceOf(InvalidIdException.class)
                .hasMessageContaining("número inteiro positivo");
    }

    @Test
    @DisplayName("Deve lançar ProductNotFoundException quando o id não existir")
    void shouldThrowProductNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Produto não encontrado");
    }
}

