package br.com.pit.cupcakes.endereco.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AddressRequest(
        @JsonProperty("destinatario") String recipient,
        @JsonProperty("cep") String postalCode,
        @JsonProperty("logradouro") String street,
        @JsonProperty("numero") String number,
        @JsonProperty("complemento") String complement,
        @JsonProperty("bairro") String neighborhood,
        @JsonProperty("cidade") String city,
        @JsonProperty("uf") String state,
        @JsonProperty("versao") @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ExactLongDeserializer.class) Long version) {
    @JsonAnySetter
    public void rejectUnknown(String key, Object value) {
        throw new IllegalArgumentException("Propriedade não permitida.");
    }
}
