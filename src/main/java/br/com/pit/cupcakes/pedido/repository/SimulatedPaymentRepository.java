package br.com.pit.cupcakes.pedido.repository;

import br.com.pit.cupcakes.pedido.entity.SimulatedPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SimulatedPaymentRepository extends JpaRepository<SimulatedPayment, Long> {

    @Query("select p from SimulatedPayment p where p.order.id = :orderId")
    Optional<SimulatedPayment> findByOrderId(@Param("orderId") Long orderId);

    @Query("select p from SimulatedPayment p where p.order.id in :orderIds")
    List<SimulatedPayment> findByOrderIdIn(@Param("orderIds") Collection<Long> orderIds);
}

