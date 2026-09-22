package com.exemplo.produtosservice.controller;

import com.exemplo.produtosservice.TestcontainersConfiguration;
import com.exemplo.produtosservice.TokenTestUtil;
import com.exemplo.produtosservice.model.Produto;
import com.exemplo.produtosservice.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestcontainersConfiguration.class)
class ProdutoControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    void deveRejeitarRequisicaoSemToken() {
        webTestClient.get().uri("/api/produtos")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.mensagem").isEqualTo("Token de acesso ausente");
    }

    @Test
    void deveRejeitarTokenComAssinaturaInvalida() {
        webTestClient.get().uri("/api/produtos")
                .headers(h -> h.setBearerAuth(TokenTestUtil.tokenComOutraChave()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRejeitarTokenExpirado() {
        webTestClient.get().uri("/api/produtos")
                .headers(h -> h.setBearerAuth(TokenTestUtil.tokenExpirado()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRejeitarRefreshTokenUsadoComoAccessToken() {
        webTestClient.get().uri("/api/produtos")
                .headers(h -> h.setBearerAuth(TokenTestUtil.refreshToken()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveListarProdutosComTokenValido() {
        webTestClient.get().uri("/api/produtos")
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Produto.class).hasSize(10);
    }

    @Test
    void deveCriarEBuscarProdutoPorId() {
        Produto criado = webTestClient.post().uri("/api/produtos")
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Produto("Impressora", new BigDecimal("850.00")))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Produto.class)
                .returnResult()
                .getResponseBody();

        webTestClient.get().uri("/api/produtos/{id}", criado.getId())
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nome").isEqualTo("Impressora")
                .jsonPath("$.preco").isEqualTo(850.00);

        produtoRepository.deleteById(criado.getId()).block();
    }

    @Test
    void deveRetornar404ParaProdutoInexistente() {
        webTestClient.get().uri("/api/produtos/{id}", 999_999)
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deveRejeitarProdutoInvalido() {
        webTestClient.post().uri("/api/produtos")
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nome\":\"\",\"preco\":0}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
