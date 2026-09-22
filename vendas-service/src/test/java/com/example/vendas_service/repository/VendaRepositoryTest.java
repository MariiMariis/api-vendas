package com.example.vendas_service.repository;

import com.example.vendas_service.TestcontainersConfiguration;
import com.example.vendas_service.models.Venda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
class VendaRepositoryTest {

    @Autowired
    private VendaRepository vendaRepository;

    @BeforeEach
    void limpar() {
        vendaRepository.deleteAll().block();
    }

    @Test
    void deveSalvarEBuscarVendaPorId() {
        var fluxo = vendaRepository.save(venda(1L, "Notebook", 2, "3500.00", "maria@teste.com", Instant.now()))
                .flatMap(salva -> vendaRepository.findById(salva.getId()));

        StepVerifier.create(fluxo)
                .assertNext(venda -> {
                    assertThat(venda.getId()).isNotNull();
                    assertThat(venda.getNomeProduto()).isEqualTo("Notebook");
                    assertThat(venda.getQuantidade()).isEqualTo(2);
                    assertThat(venda.getValorTotal()).isEqualByComparingTo("7000.00");
                })
                .verifyComplete();
    }

    @Test
    void deveListarVendasDoUsuarioDaMaisRecenteParaMaisAntiga() {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var fluxo = vendaRepository.saveAll(Flux.just(
                        venda(1L, "Notebook", 1, "3500.00", "maria@teste.com", agora.minusSeconds(60)),
                        venda(2L, "Mouse", 3, "79.90", "maria@teste.com", agora),
                        venda(3L, "Teclado", 1, "299.90", "joao@teste.com", agora)))
                .thenMany(vendaRepository.findByUsuarioOrderByDataVendaDesc("maria@teste.com"));

        StepVerifier.create(fluxo)
                .assertNext(venda -> assertThat(venda.getNomeProduto()).isEqualTo("Mouse"))
                .assertNext(venda -> assertThat(venda.getNomeProduto()).isEqualTo("Notebook"))
                .verifyComplete();
    }

    @Test
    void deveBuscarVendasPorProduto() {
        var fluxo = vendaRepository.saveAll(Flux.just(
                        venda(7L, "SSD 1TB", 1, "459.90", "maria@teste.com", Instant.now()),
                        venda(7L, "SSD 1TB", 2, "459.90", "joao@teste.com", Instant.now()),
                        venda(8L, "Webcam", 1, "199.90", "joao@teste.com", Instant.now())))
                .thenMany(vendaRepository.findByIdProduto(7L));

        StepVerifier.create(fluxo)
                .expectNextCount(2)
                .verifyComplete();
    }

    private Venda venda(Long idProduto, String nome, int quantidade, String preco, String usuario, Instant data) {
        Venda venda = new Venda();
        venda.setIdProduto(idProduto);
        venda.setNomeProduto(nome);
        venda.setQuantidade(quantidade);
        venda.setValorUnitario(new BigDecimal(preco));
        venda.setValorTotal(new BigDecimal(preco).multiply(BigDecimal.valueOf(quantidade)));
        venda.setUsuario(usuario);
        venda.setDataVenda(data);
        return venda;
    }
}
