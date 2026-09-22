package com.exemplo.authservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("refresh_token")
public class RefreshToken {

    @Id
    private Long id;

    private String tokenId;

    private Long usuarioId;

    private Instant expiraEm;

    private boolean revogado;

    public RefreshToken() {
    }

    public RefreshToken(String tokenId, Long usuarioId, Instant expiraEm) {
        this.tokenId = tokenId;
        this.usuarioId = usuarioId;
        this.expiraEm = expiraEm;
        this.revogado = false;
    }

    public boolean isValido(Instant agora) {
        return !revogado && expiraEm.isAfter(agora);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(Instant expiraEm) {
        this.expiraEm = expiraEm;
    }

    public boolean isRevogado() {
        return revogado;
    }

    public void setRevogado(boolean revogado) {
        this.revogado = revogado;
    }
}
