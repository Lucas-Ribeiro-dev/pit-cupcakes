package br.com.pit.cupcakes.pedido.controller;

import br.com.pit.cupcakes.pedido.dto.CheckoutRevisionRequest;
import br.com.pit.cupcakes.pedido.dto.CheckoutRevisionResponse;
import br.com.pit.cupcakes.pedido.service.CheckoutService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conta/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping(value = "/revisao", consumes = "application/json")
    public ResponseEntity<CheckoutRevisionResponse> createRevision(@RequestBody CheckoutRevisionRequest request,
                                                                   HttpSession session,
                                                                   Authentication authentication) {
        CheckoutRevisionResponse response = checkoutService.createRevision(request, session, authentication.getName());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}

