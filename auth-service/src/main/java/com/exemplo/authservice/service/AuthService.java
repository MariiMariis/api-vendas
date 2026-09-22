package com.exemplo.authservice.service;

import com.exemplo.authservice.dto.LoginRequest;
import com.exemplo.authservice.dto.RegistroRequest;
import com.exemplo.authservice.dto.TokenResponse;
import com.exemplo.authservice.model.RefreshToken;
import com.exemplo.authservice.model.Usuario;
import com.exemplo.authservice.repository.RefreshTokenRepository;
import com.exemplo.authservice.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";
    private static final String REFRESH_INVALIDO = "Refresh token inválido ou expirado";

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Usuario registrar(RegistroRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe um usuario cadastrado com o email " + email);
        }
        Usuario usuario = new Usuario(request.nome().trim(), email, passwordEncoder.encode(request.senha()));
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
        }

        return emitirTokens(usuario);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshToken registro = buscarRefreshValido(refreshToken);

        Usuario usuario = usuarioRepository.findById(registro.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, REFRESH_INVALIDO));

        registro.setRevogado(true);
        refreshTokenRepository.save(registro);

        return emitirTokens(usuario);
    }

    @Transactional
    public void logout(String refreshToken) {
        RefreshToken registro = buscarRefreshValido(refreshToken);
        refreshTokenRepository.revogarTodosDoUsuario(registro.getUsuarioId());
    }

    private RefreshToken buscarRefreshValido(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.validar(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, REFRESH_INVALIDO);
        }

        if (!JwtService.TIPO_REFRESH.equals(claims.get(JwtService.CLAIM_TIPO, String.class)) || claims.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, REFRESH_INVALIDO);
        }

        RefreshToken registro = refreshTokenRepository.findByTokenId(claims.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, REFRESH_INVALIDO));

        if (!registro.isValido(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, REFRESH_INVALIDO);
        }
        return registro;
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        String tokenId = UUID.randomUUID().toString();
        Instant expiraEm = jwtService.calcularExpiracaoRefresh();
        refreshTokenRepository.save(new RefreshToken(tokenId, usuario.getId(), expiraEm));

        return new TokenResponse(
                jwtService.gerarAccessToken(usuario),
                jwtService.gerarRefreshToken(usuario, tokenId, expiraEm),
                "Bearer",
                jwtService.getAccessExpiracaoSegundos(),
                jwtService.getRefreshExpiracaoSegundos());
    }
}
