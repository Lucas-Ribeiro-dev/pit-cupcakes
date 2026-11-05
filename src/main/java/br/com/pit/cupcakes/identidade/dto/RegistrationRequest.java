package br.com.pit.cupcakes.identidade.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RegistrationRequest(
        @JsonProperty("nome") String name,
        @JsonProperty("email") String email,
        @JsonProperty("senha") String password) {
    @JsonAnySetter
    public void rejectUnknown(String key, Object value) {
        throw new IllegalArgumentException("Propriedade não permitida.");
    }

    @Override
    public String toString() { return "RegistrationRequest"; }
}
