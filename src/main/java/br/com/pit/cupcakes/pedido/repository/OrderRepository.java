package br.com.pit.cupcakes.pedido.repository;

import br.com.pit.cupcakes.pedido.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o from Order o where o.user.id = :userId order by o.createdAt desc, o.id desc")
    List<Order> findByUserIdOrderByCreatedAtDescIdDesc(@Param("userId") Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByUserIdAndRevisionId(Long userId, String revisionId);

    @Query("select o from Order o join fetch o.user order by o.createdAt desc, o.id desc")
    List<Order> findAllWithUserOrderByCreatedAtDescIdDesc();

    @Query("select o from Order o join fetch o.user where o.id = :id")
    Optional<Order> findWithUserById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o join fetch o.user where o.id = :id")
    Optional<Order> findWithUserByIdForUpdate(@Param("id") Long id);
}

