package com.example.vendas_service.controllers;

import com.example.vendas_service.dto.VendaRequest;
import com.example.vendas_service.models.Venda;
import com.example.vendas_service.security.JwtAuthenticationFilter;
import com.example.vendas_service.services.VendaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    private final VendaService service;

    public VendaController(VendaService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<Venda> listarTodas() {
        return service.listarTodas();
    }

    @GetMapping("/minhas")
    public Flux<Venda> listarMinhas(ServerWebExchange exchange) {
        return service.listarDoUsuario(exchange.getAttribute(JwtAuthenticationFilter.ATRIBUTO_USUARIO));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Venda>> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Venda>> registrar(@RequestBody VendaRequest request,
                                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                 ServerWebExchange exchange) {
        String usuario = exchange.getAttribute(JwtAuthenticationFilter.ATRIBUTO_USUARIO);
        return service.registrar(request, usuario, authorization)
                .map(venda -> ResponseEntity.status(HttpStatus.CREATED).body(venda));
    }
}
