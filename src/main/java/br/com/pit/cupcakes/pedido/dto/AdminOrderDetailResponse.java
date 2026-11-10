package br.com.pit.cupcakes.pedido.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminOrderDetailResponse(
        Long id,
        @JsonProperty("revisaoId") String revisionId,
        @JsonProperty("criadoEm") Instant createdAt,
        @JsonProperty("clienteNome") String customerName,
        @JsonProperty("clienteEmail") String customerEmail,
        @JsonProperty("modalidade") String mode,
        @JsonProperty("statusAtendimento") String status,
        @JsonProperty("resultadoSimulado") String simulatedResult,
        @JsonProperty("itens") List<OrderDetailResponse.Item> items,
        @JsonProperty("endereco") OrderDetailResponse.HistoricalAddress address,
        @JsonProperty("subtotal") String subtotal,
        @JsonProperty("frete") String shippingFee,
        @JsonProperty("total") String total,
        @JsonProperty("versao") Long version,
        @JsonProperty("proximosStatusPermitidos") List<String> allowedNextStatuses
) {}

