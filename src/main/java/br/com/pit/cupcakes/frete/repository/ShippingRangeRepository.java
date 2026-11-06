package br.com.pit.cupcakes.frete.repository;

import br.com.pit.cupcakes.frete.entity.ShippingRange;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ShippingRangeRepository extends JpaRepository<ShippingRange, Long> {
    boolean existsByActiveTrue();
    @Query("select r from ShippingRange r where r.active = true and r.startPostalCode <= :cep and r.endPostalCode >= :cep")
    List<ShippingRange> matching(@Param("cep") String postalCode);
}
