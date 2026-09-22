package com.exemplo.authservice.repository;

import com.exemplo.authservice.model.Usuario;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface UsuarioRepository extends ListCrudRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}
