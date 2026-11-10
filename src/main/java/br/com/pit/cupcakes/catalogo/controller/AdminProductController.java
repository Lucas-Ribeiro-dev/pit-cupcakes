package br.com.pit.cupcakes.catalogo.controller;

import br.com.pit.cupcakes.catalogo.dto.AdminProductResponse;
import br.com.pit.cupcakes.catalogo.service.AdminProductService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/produtos")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public ResponseEntity<List<AdminProductResponse>> listAll() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(adminProductService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(adminProductService.findById(id));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<AdminProductResponse> create(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(adminProductService.create(body));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<AdminProductResponse> update(@PathVariable Long id, @RequestBody JsonNode body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(adminProductService.update(id, body));
    }
}

