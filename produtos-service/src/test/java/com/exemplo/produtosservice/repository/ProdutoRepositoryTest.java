package com.exemplo.produtosservice.repository;

import com.exemplo.produtosservice.TestcontainersConfiguration;
import com.exemplo.produtosservice.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
class ProdutoRepositoryTest {

    @Autowired
    private ProdutoRepository produtoRepository;

    @BeforeEach
    void limpar() {
        produtoRepository.deleteAll().block();
    }

    @Test
    void deveSalvarEBuscarProdutoPorId() {
        var fluxo = produtoRepository.save(new Produto("Notebook", new BigDecimal("3500.00")))
                .flatMap(salvo -> produtoRepository.findById(salvo.getId()));

        StepVerifier.create(fluxo)
                .assertNext(produto -> {
                    assertThat(produto.getId()).isNotNull();
                    assertThat(produto.getNome()).isEqualTo("Notebook");
                    assertThat(produto.getPreco()).isEqualByComparingTo("3500.00");
                })
                .verifyComplete();
    }

    @Test
    void deveListarTodosOsProdutos() {
        var fluxo = produtoRepository.saveAll(Flux.just(
                        new Produto("Mouse", new BigDecimal("79.90")),
                        new Produto("Teclado", new BigDecimal("299.90"))))
                .thenMany(produtoRepository.findAll());

        StepVerifier.create(fluxo)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void deveBuscarPorParteDoNomeIgnorandoMaiusculas() {
        var fluxo = produtoRepository.saveAll(Flux.just(
                        new Produto("Monitor 27 polegadas", new BigDecimal("1299.00")),
                        new Produto("Mouse sem fio", new BigDecimal("79.90"))))
                .thenMany(produtoRepository.findByNomeContainingIgnoreCase("MONITOR"));

        StepVerifier.create(fluxo)
                .assertNext(produto -> assertThat(produto.getNome()).isEqualTo("Monitor 27 polegadas"))
                .verifyComplete();
    }

    @Test
    void deveRetornarVazioParaIdInexistente() {
        StepVerifier.create(produtoRepository.findById(999_999L))
                .verifyComplete();
    }
}
