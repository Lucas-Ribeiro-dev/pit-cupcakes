package br.com.pit.cupcakes.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "produto")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nome", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Size(max = 255)
    @Column(name = "imagem_caminho", length = 255)
    private String imagePath;

    @NotNull
    @Column(name = "disponivel", nullable = false)
    private Boolean available = true;

    @Version
    @Column(name = "versao", nullable = false)
    private Long version = 0L;

    public Product() {
    }

    public Product(String name, String description, BigDecimal price, String imagePath, Boolean available) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imagePath = imagePath;
        this.available = available != null ? available : true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

