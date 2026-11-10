package br.com.pit.cupcakes.pedido.service;

import br.com.pit.cupcakes.pedido.entity.OrderMode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckoutRevision(
        String revisionId,
        Long userId,
        Instant createdAt,
        Instant expiresAt,
        OrderMode mode,
        List<RevisionItem> items,
        RevisionAddress address,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal total
) implements Serializable {

    public record RevisionItem(
            Long productId,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal,
            Long productVersion
    ) implements Serializable {}

    public record RevisionAddress(
            Long id,
            String recipient,
            String postalCode,
            String street,
            String number,
            String complement,
            String neighborhood,
            String city,
            String state,
            Long version
    ) implements Serializable {}
}

