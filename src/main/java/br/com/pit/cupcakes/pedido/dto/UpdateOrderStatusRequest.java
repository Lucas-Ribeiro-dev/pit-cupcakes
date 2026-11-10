package br.com.pit.cupcakes.pedido.dto;

import br.com.pit.cupcakes.endereco.dto.ExactLongDeserializer;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record UpdateOrderStatusRequest(
        @JsonProperty("statusAtendimento") String status,
        @JsonProperty("versao") @JsonDeserialize(using = ExactLongDeserializer.class) Long version
) {
    @JsonAnySetter
    public void rejectUnknown(String key, Object value) {
        throw new IllegalArgumentException("Propriedade não permitida.");
    }
}

