package br.com.pit.cupcakes.endereco.controller;

import br.com.pit.cupcakes.endereco.dto.PostalCodeResponse;
import br.com.pit.cupcakes.endereco.integration.PostalCodeClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conta/cep")
public class PostalCodeController {
    private final PostalCodeClient client;
    public PostalCodeController(PostalCodeClient client) { this.client = client; }
    @GetMapping("/{cep}") public PostalCodeResponse lookup(@PathVariable("cep") String postalCode) { return client.lookup(postalCode); }
}
