package br.com.pit.cupcakes.endereco.service;

import br.com.pit.cupcakes.endereco.dto.*;
import br.com.pit.cupcakes.endereco.entity.Address;
import br.com.pit.cupcakes.endereco.repository.AddressRepository;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AddressService {
    private final AddressRepository addresses;
    private final UserRepository users;
    public AddressService(AddressRepository addresses, UserRepository users) { this.addresses = addresses; this.users = users; }
    private User owner(String email) {
        return users.findByNormalizedEmail(email).orElseThrow(() -> new AccountOperationException(401,"NAO_AUTENTICADO","É necessário entrar na conta."));
    }
    public Address owned(Long id, String email) {
        AddressValidation.positiveId(id);
        return addresses.findByIdAndOwnerId(id, owner(email).getId()).orElseThrow(() ->
                new AccountOperationException(404,"ENDERECO_NAO_ENCONTRADO","Endereço não encontrado."));
    }
    public List<AddressResponse> list(String email) {
        return addresses.findByOwnerIdOrderById(owner(email).getId()).stream().map(AddressResponse::from).toList();
    }
    public AddressResponse detail(Long id, String email) { return AddressResponse.from(owned(id,email)); }
    @Transactional
    public AddressResponse create(AddressRequest request, String email) {
        return AddressResponse.from(addresses.saveAndFlush(new Address(owner(email), AddressValidation.normalize(request,false))));
    }
    @Transactional
    public AddressResponse update(Long id, AddressRequest request, String email) {
        AddressRequest normalized = AddressValidation.normalize(request,true);
        Address address = owned(id,email);
        checkVersion(address, normalized.version());
        address.update(normalized);
        addresses.flush();
        return AddressResponse.from(address);
    }
    @Transactional
    public void delete(Long id, Long version, String email) {
        AddressValidation.version(version);
        Address address = owned(id,email);
        checkVersion(address,version);
        addresses.delete(address);
        addresses.flush();
    }
    private void checkVersion(Address address, Long version) {
        if (!address.getVersion().equals(version)) throw new AccountOperationException(409,"ENDERECO_DESATUALIZADO","Endereço alterado em outra sessão. Revise os dados atuais antes de tentar novamente.");
    }
}
