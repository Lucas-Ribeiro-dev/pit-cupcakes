package br.com.pit.cupcakes.pedido.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OrderSummaryResponse(
        Long id,
        @JsonProperty("criadoEm") Instant createdAt,
        @JsonProperty("modalidade") String mode,
        @JsonProperty("statusAtendimento") String status,
        @JsonProperty("resultadoSimulado") String simulatedResult,
        @JsonProperty("total") String total
) {}

