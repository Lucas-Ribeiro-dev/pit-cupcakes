package br.com.pit.cupcakes.identidade.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuario")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "nome", nullable = false, length = 120)
    private String name;
    @Column(name = "email_normalizado", nullable = false, unique = true, length = 254)
    private String normalizedEmail;
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private Role role;
    @Column(name = "criado_em", nullable = false)
    private Instant createdAt;

    protected User() {}

    public User(String name, String normalizedEmail, String passwordHash) {
        this.name = name;
        this.normalizedEmail = normalizedEmail;
        this.passwordHash = passwordHash;
        this.role = Role.CLIENTE;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
