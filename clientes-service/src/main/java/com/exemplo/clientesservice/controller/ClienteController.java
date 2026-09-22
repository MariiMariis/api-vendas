package com.exemplo.clientesservice.controller;

import com.exemplo.clientesservice.model.Cliente;
import com.exemplo.clientesservice.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<Cliente> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Cliente>> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Cliente>> criar(@RequestBody Cliente cliente) {
        return service.criar(cliente)
                .map(salvo -> ResponseEntity.status(HttpStatus.CREATED).body(salvo));
    }
}
