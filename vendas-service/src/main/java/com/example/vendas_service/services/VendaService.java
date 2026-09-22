package com.example.vendas_service.services;

import com.example.vendas_service.client.ProdutoClient;
import com.example.vendas_service.dto.VendaRequest;
import com.example.vendas_service.models.Venda;
import com.example.vendas_service.repository.VendaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ProdutoClient produtoClient;

    public VendaService(VendaRepository vendaRepository, ProdutoClient produtoClient) {
        this.vendaRepository = vendaRepository;
        this.produtoClient = produtoClient;
    }

    public Flux<Venda> listarTodas() {
        return vendaRepository.findAll();
    }

    public Mono<Venda> buscarPorId(Long id) {
        return vendaRepository.findById(id);
    }

    public Flux<Venda> listarDoUsuario(String usuario) {
        return vendaRepository.findByUsuarioOrderByDataVendaDesc(usuario);
    }

    public Mono<Venda> registrar(VendaRequest request, String usuario, String authorization) {
        if (request == null || request.idProduto() == null || request.quantidade() == null || request.quantidade() <= 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "idProduto e quantidade maior que zero sao obrigatorios"));
        }

        return produtoClient.buscarPorId(request.idProduto(), authorization)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produto " + request.idProduto() + " nao encontrado")))
                .map(produto -> {
                    Venda venda = new Venda();
                    venda.setIdProduto(produto.id());
                    venda.setNomeProduto(produto.nome());
                    venda.setQuantidade(request.quantidade());
                    venda.setValorUnitario(produto.preco());
                    venda.setValorTotal(produto.preco().multiply(BigDecimal.valueOf(request.quantidade())));
                    venda.setUsuario(usuario);
                    venda.setDataVenda(Instant.now());
                    return venda;
                })
                .flatMap(vendaRepository::save);
    }
}
