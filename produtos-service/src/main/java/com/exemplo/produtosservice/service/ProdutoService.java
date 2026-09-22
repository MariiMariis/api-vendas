package com.exemplo.produtosservice.service;

import com.exemplo.produtosservice.model.Produto;
import com.exemplo.produtosservice.repository.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Regra de negocio de Produto. O controller nao fala direto com o repository,
 * fala com este service.
 */
@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public Flux<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public Mono<Produto> buscarPorId(Long id) {
        return produtoRepository.findById(id);
    }

    public Mono<Produto> criar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()
                || produto.getPreco() == null || produto.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "nome e preco positivo sao obrigatorios"));
        }
        produto.setId(null);
        return produtoRepository.save(produto);
    }
}
