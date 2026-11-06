package br.com.pit.cupcakes.endereco.repository;

import br.com.pit.cupcakes.endereco.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByOwnerIdOrderById(Long ownerId);
    Optional<Address> findByIdAndOwnerId(Long id, Long ownerId);
}
