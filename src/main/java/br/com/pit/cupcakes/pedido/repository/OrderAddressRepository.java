package br.com.pit.cupcakes.pedido.repository;

import br.com.pit.cupcakes.pedido.entity.OrderAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderAddressRepository extends JpaRepository<OrderAddress, Long> {

    Optional<OrderAddress> findByOrderId(Long orderId);
}

