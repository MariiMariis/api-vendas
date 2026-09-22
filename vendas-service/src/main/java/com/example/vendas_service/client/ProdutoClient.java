package com.example.vendas_service.client;

import com.example.vendas_service.dto.ProdutoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class ProdutoClient {

    private final WebClient webClient;

    public ProdutoClient(WebClient.Builder loadBalancedWebClientBuilder,
                         @Value("${servicos.produtos.url:http://produtos-service}") String produtosUrl) {
        this.webClient = loadBalancedWebClientBuilder.baseUrl(produtosUrl).build();
    }

    public Mono<ProdutoDTO> buscarPorId(Long idProduto, String authorization) {
        return webClient.get()
                .uri("/api/produtos/{id}", idProduto)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        resposta -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Produto " + idProduto + " nao encontrado")))
                .onStatus(status -> status.value() == HttpStatus.UNAUTHORIZED.value(),
                        resposta -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "produtos-service recusou o token")))
                .onStatus(HttpStatusCode::isError,
                        resposta -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                "Falha ao consultar produtos-service: " + resposta.statusCode().value())))
                .bodyToMono(ProdutoDTO.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(WebClientRequestException.class, e -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "produtos-service indisponivel"))
                .onErrorMap(TimeoutException.class, e -> new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT, "produtos-service nao respondeu a tempo"));
    }
}
