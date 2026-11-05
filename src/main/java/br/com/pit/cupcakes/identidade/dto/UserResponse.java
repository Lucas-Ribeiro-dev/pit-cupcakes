package br.com.pit.cupcakes.identidade.dto;

import br.com.pit.cupcakes.identidade.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponse(Long id, @JsonProperty("nome") String name, String email,
                           @JsonProperty("perfil") String role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getNormalizedEmail(), user.getRole().name());
    }
}
