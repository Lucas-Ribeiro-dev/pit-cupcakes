package br.com.pit.cupcakes.identidade.controller;

import br.com.pit.cupcakes.identidade.dto.RegistrationRequest;
import br.com.pit.cupcakes.identidade.dto.UserResponse;
import br.com.pit.cupcakes.identidade.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
    private final RegistrationService service;
    public UserController(RegistrationService service) { this.service = service; }

    @PostMapping(value = "/api/usuarios", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody RegistrationRequest request) {
        return service.register(request);
    }
}
