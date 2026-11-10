package br.com.pit.cupcakes.pedido.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckoutRevisionResponse(
        @JsonProperty("revisaoId") String revisionId,
        @JsonProperty("expiraEm") Instant expiresAt,
        @JsonProperty("modalidade") String mode,
        @JsonProperty("itens") List<Item> items,
        @JsonProperty("endereco") AddressSnapshot address,
        @JsonProperty("subtotal") String subtotal,
        @JsonProperty("frete") String shippingFee,
        @JsonProperty("total") String total
) {
    public record Item(
            @JsonProperty("produtoId") Long productId,
            @JsonProperty("nome") String name,
            @JsonProperty("precoUnitario") String unitPrice,
            @JsonProperty("quantidade") int quantity,
            @JsonProperty("subtotal") String subtotal
    ) {}

    public record AddressSnapshot(
            Long id,
            @JsonProperty("destinatario") String recipient,
            @JsonProperty("cep") String postalCode,
            @JsonProperty("logradouro") String street,
            @JsonProperty("numero") String number,
            @JsonProperty("complemento") String complement,
            @JsonProperty("bairro") String neighborhood,
            @JsonProperty("cidade") String city,
            @JsonProperty("uf") String state,
            @JsonProperty("versao") Long version
    ) {}
}

