package br.com.pit.cupcakes.frete.controller;

import br.com.pit.cupcakes.frete.dto.*;
import br.com.pit.cupcakes.frete.service.ShippingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conta/frete")
public class ShippingController {
    private final ShippingService service;
    public ShippingController(ShippingService service) { this.service = service; }
    @PostMapping("/cotacao") public ShippingResponse quote(@RequestBody ShippingRequest request, Authentication auth) { return service.quote(request,auth.getName()); }
}
