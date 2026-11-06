package br.com.pit.cupcakes.frete.dto;

import com.fasterxml.jackson.annotation.*;

public record ShippingRequest(@JsonProperty("modalidade") String mode,
        @JsonProperty("enderecoId") @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = br.com.pit.cupcakes.endereco.dto.ExactLongDeserializer.class) Long addressId) {
    @JsonAnySetter public void rejectUnknown(String key, Object value) { throw new IllegalArgumentException("Propriedade não permitida."); }
}
