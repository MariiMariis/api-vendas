package com.exemplo.produtosservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter implements WebFilter {

    public static final String ATRIBUTO_USUARIO = "usuarioAutenticado";
    private static final String PREFIXO_PROTEGIDO = "/api/";
    private static final String BEARER = "Bearer ";

    private final SecretKey chave;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String segredo) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getPath().value().startsWith(PREFIXO_PROTEGIDO)) {
            return chain.filter(exchange);
        }

        String cabecalho = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.startsWith(BEARER)) {
            return recusar(exchange, "Token de acesso ausente");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer("auth-service")
                    .build()
                    .parseSignedClaims(cabecalho.substring(BEARER.length()))
                    .getPayload();

            if (!"access".equals(claims.get("tipo", String.class))) {
                return recusar(exchange, "Token informado nao e um access token");
            }

            exchange.getAttributes().put(ATRIBUTO_USUARIO, claims.getSubject());
            return chain.filter(exchange);
        } catch (JwtException | IllegalArgumentException e) {
            return recusar(exchange, "Token invalido ou expirado");
        }
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
}
