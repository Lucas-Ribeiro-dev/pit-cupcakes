package br.com.pit.cupcakes.carrinho.controller;

import br.com.pit.cupcakes.carrinho.dto.CartResponse;
import br.com.pit.cupcakes.carrinho.service.CartOperationException;
import br.com.pit.cupcakes.carrinho.service.CartService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrinho")
public class CartController {
    private final CartService service;
    public CartController(CartService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<CartResponse> get(HttpSession session) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(session));
    }

    @PostMapping(value = "/itens", consumes = "application/json")
    public ResponseEntity<CartResponse> add(@RequestBody JsonNode body, HttpSession session) {
        requireFields(body, "produtoId", "quantidade");
        long id = jsonId(body.get("produtoId"));
        int quantity = quantity(body.get("quantidade"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.add(session, id, quantity));
    }

    @PutMapping(value = "/itens/{produtoId}", consumes = "application/json")
    public ResponseEntity<CartResponse> change(@PathVariable String produtoId, @RequestBody JsonNode body, HttpSession session) {
        long id = pathId(produtoId);
        requireFields(body, "quantidade");
        int quantity = quantity(body.get("quantidade"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.change(session, id, quantity));
    }

    @DeleteMapping("/itens/{produtoId}")
    public ResponseEntity<Void> remove(@PathVariable String produtoId, HttpSession session) {
        service.remove(session, pathId(produtoId));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private static void requireFields(JsonNode body, String... names) {
        if (body == null || !body.isObject() || body.size() != names.length)
            throw new CartOperationException(400, "DADOS_INVALIDOS", "Informe apenas os campos permitidos.");
        for (String name : names) if (!body.has(name))
            throw new CartOperationException(400, "DADOS_INVALIDOS", "Informe os campos obrigatórios.");
    }

    private static long jsonId(JsonNode value) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0)
            throw invalidId();
        return value.longValue();
    }

    private static long pathId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw invalidId();
    }

    private static CartOperationException invalidId() {
        return new CartOperationException(400, "ID_INVALIDO", "O identificador deve ser um inteiro positivo.");
    }

    private static int quantity(JsonNode value) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()
                || value.intValue() < 1 || value.intValue() > 99)
            throw new CartOperationException(400, "QUANTIDADE_INVALIDA", "A quantidade deve ficar entre 1 e 99.");
        return value.intValue();
    }
}
