package br.com.pit.cupcakes.endereco.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PostalCodeResponse(@JsonProperty("cep") String postalCode, @JsonProperty("logradouro") String street,
        @JsonProperty("bairro") String neighborhood, @JsonProperty("cidade") String city, @JsonProperty("uf") String state) {}
