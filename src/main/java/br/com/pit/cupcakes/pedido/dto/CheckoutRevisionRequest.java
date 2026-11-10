package br.com.pit.cupcakes.pedido.dto;

import br.com.pit.cupcakes.endereco.dto.ExactLongDeserializer;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record CheckoutRevisionRequest(
        @JsonProperty("modalidade") String mode,
        @JsonProperty("enderecoId") @JsonDeserialize(using = ExactLongDeserializer.class) Long addressId,
        @JsonProperty("versaoEndereco") @JsonDeserialize(using = ExactLongDeserializer.class) Long addressVersion
) {
    @JsonAnySetter
    public void rejectUnknown(String key, Object value) {
        throw new IllegalArgumentException("Propriedade não permitida.");
    }
}

