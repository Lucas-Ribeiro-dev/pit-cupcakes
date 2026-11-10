package br.com.pit.cupcakes.pedido.entity;

import br.com.pit.cupcakes.identidade.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pedido", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pedido_usuario_revisao", columnNames = {"usuario_id", "revisao_id"})
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "revisao_id", nullable = false, length = 64)
    private String revisionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade", nullable = false, length = 20)
    private OrderMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_atendimento", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "frete", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "criado_em", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "versao", nullable = false)
    private Long version = 0L;

    protected Order() {
    }

    public Order(User user, String revisionId, OrderMode mode, OrderStatus status,
                 BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total) {
        this.user = user;
        this.revisionId = revisionId;
        this.mode = mode;
        this.status = status;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.total = total;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getRevisionId() { return revisionId; }
    public OrderMode getMode() { return mode; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}

