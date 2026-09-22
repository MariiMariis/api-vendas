package com.exemplo.clientesservice;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public final class TokenTestUtil {

    public static final String SEGREDO = "00d23b30-8bd5-4cfa-935c-9305be24d5f7";

    private TokenTestUtil() {
    }

    public static String accessToken() {
        return gerar(SEGREDO, "access", Instant.now().plusSeconds(120));
    }

    public static String refreshToken() {
        return gerar(SEGREDO, "refresh", Instant.now().plusSeconds(1800));
    }

    public static String tokenExpirado() {
        return gerar(SEGREDO, "access", Instant.now().minusSeconds(60));
    }

    public static String tokenComOutraChave() {
        return gerar("outra-chave-secreta-com-tamanho-suficiente-123", "access", Instant.now().plusSeconds(120));
    }

    private static String gerar(String segredo, String tipo, Instant expiracao) {
        SecretKey chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .issuer("auth-service")
                .subject("teste@vendas.com")
                .claim("tipo", tipo)
                .issuedAt(Date.from(Instant.now().minusSeconds(300)))
                .expiration(Date.from(expiracao))
                .signWith(chave)
                .compact();
    }
}
