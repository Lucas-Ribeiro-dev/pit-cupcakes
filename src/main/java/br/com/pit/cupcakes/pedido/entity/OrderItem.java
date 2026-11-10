package br.com.pit.cupcakes.pedido.entity;

import br.com.pit.cupcakes.catalogo.entity.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido", uniqueConstraints = {
        @UniqueConstraint(name = "uk_item_pedido_produto", columnNames = {"pedido_id", "produto_id"})
})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Product product;

    @Column(name = "nome_produto", nullable = false, length = 150)
    private String productName;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantidade", nullable = false)
    private Integer quantity;

    protected OrderItem() {
    }

    public OrderItem(Order order, Product product, String productName, BigDecimal unitPrice, Integer quantity) {
        this.order = order;
        this.product = product;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Product getProduct() { return product; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
}

