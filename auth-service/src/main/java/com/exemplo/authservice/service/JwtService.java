package com.exemplo.authservice.service;

import com.exemplo.authservice.config.JwtProperties;
import com.exemplo.authservice.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    public static final String CLAIM_TIPO = "tipo";
    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";
    public static final String ISSUER = "auth-service";

    private final SecretKey chave;
    private final JwtProperties propriedades;

    public JwtService(JwtProperties propriedades) {
        this.propriedades = propriedades;
        this.chave = Keys.hmacShaKeyFor(propriedades.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String gerarAccessToken(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim(CLAIM_TIPO, TIPO_ACCESS)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(propriedades.accessExpiration())))
                .signWith(chave)
                .compact();
    }

    public String gerarRefreshToken(Usuario usuario, String tokenId, Instant expiraEm) {
        return Jwts.builder()
                .issuer(ISSUER)
                .id(tokenId)
                .subject(usuario.getEmail())
                .claim(CLAIM_TIPO, TIPO_REFRESH)
                .issuedAt(new Date())
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessExpiracaoSegundos() {
        return propriedades.accessExpiration().toSeconds();
    }

    public long getRefreshExpiracaoSegundos() {
        return propriedades.refreshExpiration().toSeconds();
    }

    public Instant calcularExpiracaoRefresh() {
        return Instant.now().plus(propriedades.refreshExpiration());
    }
}
