package br.com.pit.cupcakes.pedido.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pagamento_simulado", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pagamento_simulado_pedido", columnNames = {"pedido_id"})
})
public class SimulatedPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private SimulatedPaymentResult result;

    @Column(name = "criado_em", nullable = false)
    private Instant createdAt;

    protected SimulatedPayment() {
    }

    public SimulatedPayment(Order order, SimulatedPaymentResult result) {
        this.order = order;
        this.result = result;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public SimulatedPaymentResult getResult() { return result; }
    public Instant getCreatedAt() { return createdAt; }
}

