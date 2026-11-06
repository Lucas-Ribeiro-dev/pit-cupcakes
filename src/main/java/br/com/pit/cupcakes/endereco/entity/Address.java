package br.com.pit.cupcakes.endereco.entity;

import br.com.pit.cupcakes.endereco.dto.AddressRequest;
import br.com.pit.cupcakes.identidade.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "endereco")
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false)
    private User owner;
    @Column(name = "destinatario", nullable = false, length = 120) private String recipient;
    @Column(name = "cep", nullable = false, length = 8) private String postalCode;
    @Column(name = "logradouro", nullable = false, length = 160) private String street;
    @Column(name = "numero", nullable = false, length = 20) private String number;
    @Column(name = "complemento", length = 120) private String complement;
    @Column(name = "bairro", nullable = false, length = 100) private String neighborhood;
    @Column(name = "cidade", nullable = false, length = 100) private String city;
    @Column(name = "uf", nullable = false, length = 2) private String state;
    @Version @Column(name = "versao", nullable = false) private Long version;
    protected Address() {}
    public Address(User owner, AddressRequest request) { this.owner = owner; update(request); }
    public void update(AddressRequest request) {
        recipient = request.recipient(); postalCode = request.postalCode(); street = request.street();
        number = request.number(); complement = request.complement(); neighborhood = request.neighborhood();
        city = request.city(); state = request.state();
    }
    public Long getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getPostalCode() { return postalCode; }
    public String getStreet() { return street; }
    public String getNumber() { return number; }
    public String getComplement() { return complement; }
    public String getNeighborhood() { return neighborhood; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public Long getVersion() { return version; }
}
