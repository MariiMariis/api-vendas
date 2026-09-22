package com.exemplo.authservice.repository;

import com.exemplo.authservice.model.RefreshToken;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends ListCrudRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(String tokenId);

    List<RefreshToken> findByUsuarioIdAndRevogadoFalse(Long usuarioId);

    @Modifying
    @Query("UPDATE refresh_token SET revogado = TRUE WHERE usuario_id = :usuarioId AND revogado = FALSE")
    int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId);
}
