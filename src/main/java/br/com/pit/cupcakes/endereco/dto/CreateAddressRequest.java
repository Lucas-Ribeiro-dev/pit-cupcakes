package br.com.pit.cupcakes.endereco.dto;

import com.fasterxml.jackson.annotation.*;

public record CreateAddressRequest(@JsonProperty("destinatario") String recipient,
        @JsonProperty("cep") String postalCode, @JsonProperty("logradouro") String street,
        @JsonProperty("numero") String number, @JsonProperty("complemento") String complement,
        @JsonProperty("bairro") String neighborhood, @JsonProperty("cidade") String city, @JsonProperty("uf") String state) {
    @JsonAnySetter public void rejectUnknown(String key, Object value) { throw new IllegalArgumentException("Propriedade não permitida."); }
    public AddressRequest toAddressRequest() { return new AddressRequest(recipient,postalCode,street,number,complement,neighborhood,city,state,null); }
}
