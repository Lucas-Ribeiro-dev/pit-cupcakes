package br.com.pit.cupcakes.frete.dto;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShippingResponse(@JsonProperty("modalidade") String mode, @JsonProperty("enderecoId") Long addressId,
        @JsonProperty("versaoEndereco") Long addressVersion, @JsonProperty("valor") String amount) {}
