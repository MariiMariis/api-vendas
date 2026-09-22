package com.example.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

// @Component: o Spring acha esta classe sozinho e passa a usa-la.
// GlobalFilter: vale para TODAS as rotas do gateway, sem precisar listar uma a uma.
@Component
public class TokenFilter implements GlobalFilter, Ordered {

    public static final String CABECALHO_USUARIO = "X-Usuario-Email";

    // As unicas rotas que passam sem token. Sem elas ninguem consegue se
    // cadastrar nem pegar o primeiro token -- o sistema tranca por fora.
    private static final List<String> LIVRES = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",
            "/api/auth/logout");

    private final SecretKey chave;

    // @Value pega a chave das configuracoes. hmacShaKeyFor transforma o texto
    // em chave de verdade. E' a MESMA do auth-service: la assina, aqui confere.
    public TokenFilter(@Value("${jwt.secret}") String segredo) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    // Este metodo roda a cada requisicao que chega no gateway.
    // exchange = a requisicao e a resposta. chain = a fila do que vem depois.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String caminho = exchange.getRequest().getURI().getPath();

        // Rota livre: chain.filter e' o "pode seguir", sem conferir nada.
        if (LIVRES.contains(caminho)) {
            return chain.filter(exchange);
        }

        // Le o cabecalho onde o crachá viaja: "Authorization: Bearer eyJhbGci..."
        String cabecalho = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Nao mandou cabecalho, ou mandou em outro formato: nem olha o token.
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            return recusar(exchange, "Token de acesso ausente");
        }

        Claims claims;
        try {
            // substring(7) corta o "Bearer " (7 letras) e deixa so' o token.
            // parseSignedClaims confere a assinatura com a nossa chave e
            // estoura excecao se o token for falso ou tiver sido alterado.
            claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer("auth-service")
                    .build()
                    .parseSignedClaims(cabecalho.substring(7))
                    .getPayload();
        } catch (Exception e) {
            return recusar(exchange, "Token invalido ou expirado");
        }

        if (!"access".equals(claims.get("tipo", String.class))) {
            return recusar(exchange, "Token informado nao e um access token");
        }

        ServerWebExchange autenticado = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .headers(headers -> headers.set(CABECALHO_USUARIO, claims.getSubject()))
                        .build())
                .build();

        // Token conferido: a requisicao segue para o servico de destino.
        return chain.filter(autenticado);
    }

    private Mono<Void> recusar(ServerWebExchange exchange, String mensagem) {
        var resposta = exchange.getResponse();
        resposta.setStatusCode(HttpStatus.UNAUTHORIZED);
        resposta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        resposta.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        String corpo = "{\"status\":401,\"erro\":\"Unauthorized\",\"mensagem\":\"" + mensagem + "\"}";
        DataBuffer buffer = resposta.bufferFactory().wrap(corpo.getBytes(StandardCharsets.UTF_8));
        return resposta.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
