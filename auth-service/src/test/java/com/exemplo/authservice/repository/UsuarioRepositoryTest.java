package com.exemplo.authservice.repository;

import com.exemplo.authservice.TestcontainersConfiguration;
import com.exemplo.authservice.model.RefreshToken;
import com.exemplo.authservice.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void limpar() {
        refreshTokenRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveSalvarEBuscarUsuarioPorEmail() {
        Usuario salvo = usuarioRepository.save(new Usuario("Maria", "maria@teste.com", "hash"));

        Optional<Usuario> encontrado = usuarioRepository.findByEmail("maria@teste.com");

        assertThat(salvo.getId()).isNotNull();
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Maria");
        assertThat(usuarioRepository.existsByEmail("maria@teste.com")).isTrue();
        assertThat(usuarioRepository.existsByEmail("outro@teste.com")).isFalse();
    }

    @Test
    void deveSalvarRefreshTokenEBuscarPorTokenId() {
        Usuario usuario = usuarioRepository.save(new Usuario("Joao", "joao@teste.com", "hash"));
        Instant expiraEm = Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);

        refreshTokenRepository.save(new RefreshToken("token-123", usuario.getId(), expiraEm));

        Optional<RefreshToken> encontrado = refreshTokenRepository.findByTokenId("token-123");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(encontrado.get().getExpiraEm()).isEqualTo(expiraEm);
        assertThat(encontrado.get().isValido(Instant.now())).isTrue();
    }

    @Test
    void deveRevogarTodosOsRefreshTokensDoUsuario() {
        Usuario usuario = usuarioRepository.save(new Usuario("Ana", "ana@teste.com", "hash"));
        Instant expiraEm = Instant.now().plus(30, ChronoUnit.MINUTES);
        refreshTokenRepository.save(new RefreshToken("t1", usuario.getId(), expiraEm));
        refreshTokenRepository.save(new RefreshToken("t2", usuario.getId(), expiraEm));

        int revogados = refreshTokenRepository.revogarTodosDoUsuario(usuario.getId());

        assertThat(revogados).isEqualTo(2);
        assertThat(refreshTokenRepository.findByUsuarioIdAndRevogadoFalse(usuario.getId())).isEmpty();
        assertThat(refreshTokenRepository.findByTokenId("t1")).get().extracting(RefreshToken::isRevogado).isEqualTo(true);
    }
}
