package br.com.pit.cupcakes.frete.repository;

import br.com.pit.cupcakes.frete.entity.ShippingRange;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ShippingRangeRepository extends JpaRepository<ShippingRange, Long> {
    boolean existsByActiveTrue();
    @Query("select r from ShippingRange r where r.active = true and r.startPostalCode <= :cep and r.endPostalCode >= :cep")
    List<ShippingRange> matching(@Param("cep") String postalCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ShippingRange r where r.active = true and r.startPostalCode <= :cep and r.endPostalCode >= :cep")
    List<ShippingRange> matchingForUpdate(@Param("cep") String postalCode);
}
