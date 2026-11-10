package br.com.pit.cupcakes.pedido.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfirmOrderRequest(
        @JsonProperty("revisaoId") String revisionId,
        @JsonProperty("resultadoSimulado") String simulatedResult
) {
    @JsonAnySetter
    public void rejectUnknown(String key, Object value) {
        throw new IllegalArgumentException("Propriedade não permitida.");
    }
}

