package com.exemplo.authservice.service;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.exemplo.authservice.model.Usuario;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service 
public class JwtToken {

    private  SecretKey secretKey;

    public JwtToken(@Value("${jwt.secret}") String segredo) {
        secretKey = Keys.hmacShaKeyFor(segredo.getBytes());
    }

    public String gerarToken(Usuario usuario){
        return Jwts.builder()
                .subject(usuario.getEmail())
                .signWith(secretKey).compact();
    }
    
}
