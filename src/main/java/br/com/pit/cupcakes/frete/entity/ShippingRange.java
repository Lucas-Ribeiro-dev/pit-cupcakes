package br.com.pit.cupcakes.frete.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "faixa_frete")
public class ShippingRange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "cep_inicio", nullable = false, length = 8) private String startPostalCode;
    @Column(name = "cep_fim", nullable = false, length = 8) private String endPostalCode;
    @Column(name = "valor", nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "ativa", nullable = false) private boolean active;
    protected ShippingRange() {}
    public ShippingRange(String startPostalCode, String endPostalCode, BigDecimal amount, boolean active) {
        this.startPostalCode = startPostalCode;
        this.endPostalCode = endPostalCode;
        this.amount = amount;
        this.active = active;
    }
    public Long getId() { return id; }
    public String getStartPostalCode() { return startPostalCode; }
    public String getEndPostalCode() { return endPostalCode; }
    public BigDecimal getAmount() { return amount; }
    public boolean isActive() { return active; }
}
