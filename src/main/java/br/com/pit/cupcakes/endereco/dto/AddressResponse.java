package br.com.pit.cupcakes.endereco.dto;

import br.com.pit.cupcakes.endereco.entity.Address;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AddressResponse(Long id, @JsonProperty("destinatario") String recipient,
        @JsonProperty("cep") String postalCode, @JsonProperty("logradouro") String street,
        @JsonProperty("numero") String number, @JsonProperty("complemento") String complement,
        @JsonProperty("bairro") String neighborhood, @JsonProperty("cidade") String city,
        @JsonProperty("uf") String state, @JsonProperty("versao") Long version) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(address.getId(), address.getRecipient(), address.getPostalCode(), address.getStreet(),
                address.getNumber(), address.getComplement(), address.getNeighborhood(), address.getCity(), address.getState(), address.getVersion());
    }
}
