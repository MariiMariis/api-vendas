package com.exemplo.authservice.config;

import com.exemplo.authservice.model.Usuario;
import com.exemplo.authservice.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "auth.demo", name = "email")
public class UsuarioDemoInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;

    public UsuarioDemoInitializer(UsuarioRepository usuarioRepository,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${auth.demo.nome}") String nome,
                                  @Value("${auth.demo.email}") String email,
                                  @Value("${auth.demo.senha}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email.trim().toLowerCase();
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!usuarioRepository.existsByEmail(email)) {
            usuarioRepository.save(new Usuario(nome, email, passwordEncoder.encode(senha)));
        }
    }
}
