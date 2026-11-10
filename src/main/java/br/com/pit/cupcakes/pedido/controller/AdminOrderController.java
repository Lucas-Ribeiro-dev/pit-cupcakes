package br.com.pit.cupcakes.pedido.controller;

import br.com.pit.cupcakes.pedido.dto.AdminOrderDetailResponse;
import br.com.pit.cupcakes.pedido.dto.AdminOrderSummaryResponse;
import br.com.pit.cupcakes.pedido.dto.UpdateOrderStatusRequest;
import br.com.pit.cupcakes.pedido.service.OrderService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pedidos")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<AdminOrderSummaryResponse>> listOrders() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(orderService.listAdminOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderDetailResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(orderService.getAdminOrderDetail(id));
    }

    @PutMapping(value = "/{id}/status", consumes = "application/json")
    public ResponseEntity<AdminOrderDetailResponse> updateStatus(@PathVariable Long id,
                                                                 @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(orderService.updateOrderStatus(id, request));
    }
}

