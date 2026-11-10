package br.com.pit.cupcakes.pedido.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "endereco_pedido")
public class OrderAddress {

    @Id
    @Column(name = "pedido_id")
    private Long orderId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "pedido_id", nullable = false)
    private Order order;

    @Column(name = "destinatario", nullable = false, length = 120)
    private String recipient;

    @Column(name = "cep", nullable = false, length = 8)
    private String postalCode;

    @Column(name = "logradouro", nullable = false, length = 160)
    private String street;

    @Column(name = "numero", nullable = false, length = 20)
    private String number;

    @Column(name = "complemento", length = 120)
    private String complement;

    @Column(name = "bairro", nullable = false, length = 100)
    private String neighborhood;

    @Column(name = "cidade", nullable = false, length = 100)
    private String city;

    @Column(name = "uf", nullable = false, length = 2)
    private String state;

    protected OrderAddress() {
    }

    public OrderAddress(Order order, String recipient, String postalCode, String street, String number,
                        String complement, String neighborhood, String city, String state) {
        this.order = order;
        this.recipient = recipient;
        this.postalCode = postalCode;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
    }

    public Long getOrderId() { return orderId; }
    public Order getOrder() { return order; }
    public String getRecipient() { return recipient; }
    public String getPostalCode() { return postalCode; }
    public String getStreet() { return street; }
    public String getNumber() { return number; }
    public String getComplement() { return complement; }
    public String getNeighborhood() { return neighborhood; }
    public String getCity() { return city; }
    public String getState() { return state; }
}

