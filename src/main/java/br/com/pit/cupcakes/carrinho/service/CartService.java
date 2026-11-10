package br.com.pit.cupcakes.carrinho.service;

import br.com.pit.cupcakes.carrinho.dto.CartResponse;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class CartService {
    private static final String CART_ATTRIBUTE = "cartQuantities";
    private final ProductRepository products;

    public CartService(ProductRepository products) { this.products = products; }

    @Transactional(readOnly = true)
    public CartResponse get(HttpSession session) {
        Map<Long, Integer> snapshot;
        synchronized (session) { snapshot = new TreeMap<>(cart(session)); }
        return response(snapshot);
    }

    @Transactional(readOnly = true)
    public CartResponse add(HttpSession session, long id, int quantity) {
        synchronized (session) {
            Map<Long, Integer> cart = cart(session);
            Product product = products.findById(id).orElseThrow(() -> new CartOperationException(404,
                    "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado."));
            if (!Boolean.TRUE.equals(product.getAvailable())) throw new CartOperationException(409,
                    "PRODUTO_INDISPONIVEL", "Produto indisponível.");
            if (!cart.containsKey(id) && cart.size() >= 50) throw new CartOperationException(400,
                    "LIMITE_CARRINHO_ATINGIDO", "O carrinho aceita até 50 produtos diferentes.");
            int updated = cart.getOrDefault(id, 0) + quantity;
            if (updated > 99) throw new CartOperationException(400,
                    "QUANTIDADE_INVALIDA", "A quantidade por produto deve ficar entre 1 e 99.");
            cart.put(id, updated);
            return response(new TreeMap<>(cart));
        }
    }

    @Transactional(readOnly = true)
    public CartResponse change(HttpSession session, long id, int quantity) {
        synchronized (session) {
            Map<Long, Integer> cart = cart(session);
            if (!cart.containsKey(id)) throw new CartOperationException(404,
                    "ITEM_NAO_ENCONTRADO", "Item não encontrado no carrinho.");
            Product product = products.findById(id).orElse(null);
            if (product == null || !Boolean.TRUE.equals(product.getAvailable())) throw new CartOperationException(409,
                    "ITEM_BLOQUEADO", "Remova o item indisponível do carrinho.");
            cart.put(id, quantity);
            return response(new TreeMap<>(cart));
        }
    }

    public void remove(HttpSession session, long id) {
        synchronized (session) { cart(session).remove(id); }
    }

    public Map<Long, Integer> snapshot(HttpSession session) {
        synchronized (session) { return new TreeMap<>(cart(session)); }
    }

    public void clear(HttpSession session) {
        synchronized (session) { cart(session).clear(); }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> cart(HttpSession session) {
        Object stored = session.getAttribute(CART_ATTRIBUTE);
        if (stored != null) return (Map<Long, Integer>) stored;
        Map<Long, Integer> created = new HashMap<>();
        session.setAttribute(CART_ATTRIBUTE, created);
        return created;
    }

    private CartResponse response(Map<Long, Integer> quantities) {
        Map<Long, Product> current = products.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        BigDecimal total = BigDecimal.ZERO;
        boolean blocked = false;
        List<CartResponse.Item> items = new java.util.ArrayList<>();
        int totalQuantity = 0;
        for (var entry : quantities.entrySet()) {
            long id = entry.getKey();
            int quantity = entry.getValue();
            totalQuantity += quantity;
            Product product = current.get(id);
            if (product == null) {
                blocked = true;
                items.add(new CartResponse.Item(id, "Produto não encontrado", null, quantity, false,
                        "PRODUTO_NAO_ENCONTRADO", null, null));
            } else {
                String price = money(product.getPrice());
                boolean available = Boolean.TRUE.equals(product.getAvailable());
                String lineTotal = null;
                if (available) {
                    BigDecimal value = product.getPrice().multiply(BigDecimal.valueOf(quantity));
                    total = total.add(value);
                    lineTotal = money(value);
                } else blocked = true;
                items.add(new CartResponse.Item(id, product.getName(), product.getImagePath(), quantity,
                        available, available ? null : "PRODUTO_INDISPONIVEL", price, lineTotal));
            }
        }
        return new CartResponse(items, totalQuantity, money(total), blocked, items.isEmpty());
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
