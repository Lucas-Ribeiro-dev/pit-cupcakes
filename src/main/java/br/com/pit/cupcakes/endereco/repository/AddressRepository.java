package br.com.pit.cupcakes.endereco.repository;

import br.com.pit.cupcakes.endereco.entity.Address;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByOwnerIdOrderById(Long ownerId);
    Optional<Address> findByIdAndOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Address a where a.id = :id and a.owner.id = :ownerId")
    Optional<Address> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
