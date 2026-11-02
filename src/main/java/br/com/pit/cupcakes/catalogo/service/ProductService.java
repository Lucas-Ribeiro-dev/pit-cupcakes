package br.com.pit.cupcakes.catalogo.service;

import br.com.pit.cupcakes.catalogo.dto.ProductResponse;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.compartilhado.exception.InvalidIdException;
import br.com.pit.cupcakes.compartilhado.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> findAll() {
        List<Product> products = productRepository.findAllByOrderByIdAsc();
        return products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public ProductResponse findById(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidIdException("O identificador informado deve ser um número inteiro positivo.");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado para o identificador informado."));

        return ProductResponse.fromEntity(product);
    }
}

