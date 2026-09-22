package com.example.vendas_service.controllers;

import com.example.vendas_service.TestcontainersConfiguration;
import com.example.vendas_service.TokenTestUtil;
import com.example.vendas_service.repository.VendaRepository;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestcontainersConfiguration.class)
class VendaFluxoIntegrationTest {

    private static final AtomicReference<String> AUTORIZACAO_RECEBIDA = new AtomicReference<>();
    private static final AtomicInteger CHAMADAS_PRODUTOS = new AtomicInteger();

    private static final DisposableServer PRODUTOS_FAKE = HttpServer.create()
            .port(0)
            .route(rotas -> rotas.get("/api/produtos/{id}", (requisicao, resposta) -> {
                CHAMADAS_PRODUTOS.incrementAndGet();
                AUTORIZACAO_RECEBIDA.set(requisicao.requestHeaders().get("Authorization"));
                if ("1".equals(requisicao.param("id"))) {
                    return resposta.header("Content-Type", "application/json")
                            .sendString(Mono.just("{\"id\":1,\"nome\":\"Notebook\",\"preco\":3500.00}"));
                }
                return resposta.status(HttpResponseStatus.NOT_FOUND).send();
            }))
            .bindNow();

    @DynamicPropertySource
    static void configurarProdutosService(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.produtos-service[0].uri",
                () -> "http://localhost:" + PRODUTOS_FAKE.port());
    }

    @AfterAll
    static void pararProdutosFake() {
        PRODUTOS_FAKE.disposeNow();
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private VendaRepository vendaRepository;

    @BeforeEach
    void limpar() {
        vendaRepository.deleteAll().block();
        AUTORIZACAO_RECEBIDA.set(null);
        CHAMADAS_PRODUTOS.set(0);
    }

    @Test
    void deveRejeitarVendaSemToken() {
        webTestClient.post().uri("/api/vendas")
                .bodyValue("{\"idProduto\":1,\"quantidade\":2}")
                .header("Content-Type", "application/json")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(CHAMADAS_PRODUTOS.get()).isZero();
    }

    @Test
    void deveRejeitarTokenExpirado() {
        webTestClient.get().uri("/api/vendas")
                .headers(h -> h.setBearerAuth(TokenTestUtil.tokenExpirado()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRegistrarVendaConsultandoProdutosViaWebClientERepassandoToken() {
        String token = TokenTestUtil.accessToken();

        webTestClient.post().uri("/api/vendas")
                .headers(h -> h.setBearerAuth(token))
                .header("Content-Type", "application/json")
                .bodyValue("{\"idProduto\":1,\"quantidade\":2}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.idProduto").isEqualTo(1)
                .jsonPath("$.nomeProduto").isEqualTo("Notebook")
                .jsonPath("$.quantidade").isEqualTo(2)
                .jsonPath("$.valorUnitario").isEqualTo(3500.0)
                .jsonPath("$.valorTotal").isEqualTo(7000.0)
                .jsonPath("$.usuario").isEqualTo("teste@vendas.com");

        assertThat(CHAMADAS_PRODUTOS.get()).isEqualTo(1);
        assertThat(AUTORIZACAO_RECEBIDA.get()).isEqualTo("Bearer " + token);

        webTestClient.get().uri("/api/vendas/minhas")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].nomeProduto").isEqualTo("Notebook");
    }

    @Test
    void deveRetornar404QuandoProdutoNaoExiste() {
        webTestClient.post().uri("/api/vendas")
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .header("Content-Type", "application/json")
                .bodyValue("{\"idProduto\":99,\"quantidade\":1}")
                .exchange()
                .expectStatus().isNotFound();

        assertThat(vendaRepository.count().block()).isZero();
    }

    @Test
    void deveRejeitarQuantidadeInvalidaSemChamarProdutos() {
        webTestClient.post().uri("/api/vendas")
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .header("Content-Type", "application/json")
                .bodyValue("{\"idProduto\":1,\"quantidade\":0}")
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(CHAMADAS_PRODUTOS.get()).isZero();
    }
}
