package com.exemplo.authservice.controller;

import com.exemplo.authservice.dto.LoginRequest;
import com.exemplo.authservice.dto.UsuarioRequest;
import com.exemplo.authservice.model.Usuario;
import com.exemplo.authservice.service.JwtToken;
import com.exemplo.authservice.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;
    private final JwtToken jwtService;

    @PostMapping
    public ResponseEntity<Long> cadastrar(@RequestBody UsuarioRequest request) {
        Usuario usuario = service.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        Usuario usuario = this.service.autenticar(request);
        String token = jwtService.gerarToken(usuario);
        return ResponseEntity.ok(token);
    }
}
