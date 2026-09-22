package com.example.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TokenFilterTest {

    private static final String SEGREDO = "00d23b30-8bd5-4cfa-935c-9305be24d5f7";

    private final TokenFilter filtro = new TokenFilter(SEGREDO);
    private final AtomicReference<ServerWebExchange> repassado = new AtomicReference<>();
    private final GatewayFilterChain cadeia = exchange -> {
        repassado.set(exchange);
        return Mono.empty();
    };

    @Test
    void deveLiberarRotasPublicasDeAutenticacao() {
        for (String rota : new String[]{"/api/auth/login", "/api/auth/refresh", "/api/auth/register"}) {
            repassado.set(null);
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(rota));

            StepVerifier.create(filtro.filter(exchange, cadeia)).verifyComplete();

            assertThat(repassado.get()).isNotNull();
        }
    }

    @Test
    void deveRejeitarRotaProtegidaSemToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes"));

        StepVerifier.create(filtro.filter(exchange, cadeia)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repassado.get()).isNull();
    }

    @Test
    void deveRejeitarTokenAssinadoComOutraChave() {
        String token = token("outra-chave-secreta-com-tamanho-suficiente-123", "access", 120);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        StepVerifier.create(filtro.filter(exchange, cadeia)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRejeitarTokenExpirado() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/vendas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SEGREDO, "access", -60)));

        StepVerifier.create(filtro.filter(exchange, cadeia)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRejeitarRefreshTokenEmRotaProtegida() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/produtos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SEGREDO, "refresh", 1800)));

        StepVerifier.create(filtro.filter(exchange, cadeia)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repassado.get()).isNull();
    }

    @Test
    void deveRepassarRequisicaoComAccessTokenValido() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/produtos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SEGREDO, "access", 120)));

        StepVerifier.create(filtro.filter(exchange, cadeia)).verifyComplete();

        assertThat(repassado.get()).isNotNull();
        assertThat(repassado.get().getRequest().getHeaders().getFirst(TokenFilter.CABECALHO_USUARIO))
                .isEqualTo("teste@vendas.com");
    }

    private String token(String segredo, String tipo, long segundosParaExpirar) {
        return Jwts.builder()
                .issuer("auth-service")
                .subject("teste@vendas.com")
                .claim("tipo", tipo)
                .issuedAt(Date.from(Instant.now().minusSeconds(300)))
                .expiration(Date.from(Instant.now().plusSeconds(segundosParaExpirar)))
                .signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
