package br.com.pit.cupcakes.catalogo.dto;

import br.com.pit.cupcakes.catalogo.entity.Product;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.RoundingMode;

public record AdminProductResponse(
        Long id,
        @JsonProperty("nome") String name,
        @JsonProperty("descricao") String description,
        @JsonProperty("preco") String price,
        @JsonProperty("imagemCaminho") String imagePath,
        @JsonProperty("imagemUrl") String imageUrl,
        @JsonProperty("disponivel") boolean available,
        @JsonProperty("versao") Long version
) {
    public static AdminProductResponse fromEntity(Product product) {
        String formattedPrice = product.getPrice() != null
                ? product.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString()
                : "0.00";
        return new AdminProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                formattedPrice,
                product.getImagePath(),
                product.getImagePath(),
                Boolean.TRUE.equals(product.getAvailable()),
                product.getVersion()
        );
    }
}

