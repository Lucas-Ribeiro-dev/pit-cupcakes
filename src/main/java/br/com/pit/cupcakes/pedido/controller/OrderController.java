package br.com.pit.cupcakes.pedido.controller;

import br.com.pit.cupcakes.pedido.dto.ConfirmOrderRequest;
import br.com.pit.cupcakes.pedido.dto.OrderDetailResponse;
import br.com.pit.cupcakes.pedido.dto.OrderSummaryResponse;
import br.com.pit.cupcakes.pedido.service.CheckoutService;
import br.com.pit.cupcakes.pedido.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conta/pedidos")
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    public OrderController(CheckoutService checkoutService, OrderService orderService) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<OrderDetailResponse> confirmOrder(@RequestBody ConfirmOrderRequest request,
                                                            HttpSession session,
                                                            Authentication authentication) {
        CheckoutService.ConfirmationOutcome outcome = checkoutService.confirmOrder(request, session, authentication.getName());
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(outcome.order());
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> listOrders(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(orderService.listCustomerOrders(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrder(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(orderService.getCustomerOrderDetail(id, authentication.getName()));
    }
}

