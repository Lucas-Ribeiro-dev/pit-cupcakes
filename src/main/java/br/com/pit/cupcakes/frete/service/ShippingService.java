package br.com.pit.cupcakes.frete.service;

import br.com.pit.cupcakes.endereco.service.*;
import br.com.pit.cupcakes.frete.dto.*;
import br.com.pit.cupcakes.frete.repository.ShippingRangeRepository;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShippingService {
    private final AddressService addresses;
    private final ShippingRangeRepository ranges;
    public ShippingService(AddressService addresses, ShippingRangeRepository ranges) { this.addresses = addresses; this.ranges = ranges; }
    public ShippingResponse quote(ShippingRequest request, String email) {
        if ("RETIRADA".equals(request.mode()) && request.addressId() == null) return new ShippingResponse("RETIRADA",null,null,"0.00");
        if (!"ENTREGA".equals(request.mode())) throw new ValidationException();
        var address = addresses.owned(request.addressId(),email);
        if (!"DF".equals(address.getState())) throw outside();
        if (!ranges.existsByActiveTrue()) throw new AccountOperationException(422,"ENTREGA_NAO_CONFIGURADA","Entrega ainda não configurada. Retirada gratuita disponível.");
        var matching = ranges.matching(address.getPostalCode());
        if (matching.isEmpty()) throw outside();
        if (matching.size() != 1) throw new IllegalStateException("Configuração de frete ambígua.");
        return new ShippingResponse("ENTREGA",address.getId(),address.getVersion(),matching.getFirst().getAmount().setScale(2).toPlainString());
    }
    private AccountOperationException outside() {
        return new AccountOperationException(422,"FORA_DA_AREA_DE_ENTREGA","Endereço fora da área de entrega. Retirada gratuita disponível.");
    }
}
