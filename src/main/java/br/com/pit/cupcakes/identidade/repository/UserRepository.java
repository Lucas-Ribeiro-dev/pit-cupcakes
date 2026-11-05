package br.com.pit.cupcakes.identidade.repository;

import br.com.pit.cupcakes.identidade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNormalizedEmail(String normalizedEmail);
    boolean existsByNormalizedEmail(String normalizedEmail);
}
