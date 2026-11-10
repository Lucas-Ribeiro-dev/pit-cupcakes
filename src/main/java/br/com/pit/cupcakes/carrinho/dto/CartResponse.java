package br.com.pit.cupcakes.carrinho.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CartResponse(@JsonProperty("itens") List<Item> items,
        @JsonProperty("quantidadeTotal") int totalQuantity,
        @JsonProperty("subtotal") String subtotal,
        @JsonProperty("possuiItensBloqueados") boolean hasBlockedItems,
        @JsonProperty("vazio") boolean empty) {
    public record Item(@JsonProperty("produtoId") Long productId,
            @JsonProperty("nome") String name,
            @JsonProperty("imagemUrl") String imageUrl,
            @JsonProperty("quantidade") int quantity,
            @JsonProperty("disponivel") boolean available,
            @JsonProperty("motivoBloqueio") String blockingReason,
            @JsonProperty("precoUnitario") String unitPrice,
            @JsonProperty("subtotal") String subtotal) {}
}
