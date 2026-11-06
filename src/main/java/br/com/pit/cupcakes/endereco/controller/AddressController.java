package br.com.pit.cupcakes.endereco.controller;

import br.com.pit.cupcakes.endereco.dto.*;
import br.com.pit.cupcakes.endereco.service.AddressService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conta/enderecos")
public class AddressController {
    private final AddressService service;
    public AddressController(AddressService service) { this.service = service; }
    @GetMapping public List<AddressResponse> list(Authentication auth) { return service.list(auth.getName()); }
    @GetMapping("/{id}") public AddressResponse detail(@PathVariable Long id, Authentication auth) { return service.detail(id,auth.getName()); }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AddressResponse create(@RequestBody CreateAddressRequest request, Authentication auth) { return service.create(request.toAddressRequest(),auth.getName()); }
    @PutMapping("/{id}") public AddressResponse update(@PathVariable Long id, @RequestBody AddressRequest request, Authentication auth) { return service.update(id,request,auth.getName()); }
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam("versao") Long version, Authentication auth) { service.delete(id,version,auth.getName()); }
}
