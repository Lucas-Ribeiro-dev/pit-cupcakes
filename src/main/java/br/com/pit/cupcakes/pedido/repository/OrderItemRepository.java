package br.com.pit.cupcakes.pedido.repository;

import br.com.pit.cupcakes.pedido.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select i from OrderItem i where i.order.id = :orderId order by i.product.id asc, i.id asc")
    List<OrderItem> findByOrderIdOrderByProductIdAsc(@Param("orderId") Long orderId);
}

