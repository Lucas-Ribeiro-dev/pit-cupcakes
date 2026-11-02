package br.com.pit.cupcakes.compartilhado.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(
        @JsonProperty("codigo") String code,
        @JsonProperty("mensagem") String message
) {
}

