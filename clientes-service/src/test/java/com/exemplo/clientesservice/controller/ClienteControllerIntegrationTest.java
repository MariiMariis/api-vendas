package com.exemplo.clientesservice.controller;

import com.exemplo.clientesservice.TestcontainersConfiguration;
import com.exemplo.clientesservice.TokenTestUtil;
import com.exemplo.clientesservice.model.Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestcontainersConfiguration.class)
class ClienteControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void deveRejeitarRequisicaoSemToken() {
        webTestClient.get().uri("/api/clientes")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRejeitarTokenInvalido() {
        webTestClient.get().uri("/api/clientes")
                .header("Authorization", "Bearer abc.def.ghi")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveListarClientesSemeadosComTokenValido() {
        webTestClient.get().uri("/api/clientes")
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.email == 'ana.souza@exemplo.com' && @.nome == 'Ana Souza')]").exists();
    }

    @Test
    void deveCriarClienteERejeitarEmailDuplicado() {
        Cliente novo = new Cliente("Mariana Teste", "mariana.teste@exemplo.com");

        webTestClient.post().uri("/api/clientes")
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .bodyValue(novo)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.email").isEqualTo("mariana.teste@exemplo.com");

        webTestClient.post().uri("/api/clientes")
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .bodyValue(novo)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void deveRetornar404ParaClienteInexistente() {
        webTestClient.get().uri("/api/clientes/{id}", 999_999)
                .headers(h -> h.setBearerAuth(TokenTestUtil.accessToken()))
                .exchange()
                .expectStatus().isNotFound();
    }
}
