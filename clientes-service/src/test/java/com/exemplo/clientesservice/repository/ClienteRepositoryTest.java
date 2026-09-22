package com.exemplo.clientesservice.repository;

import com.exemplo.clientesservice.TestcontainersConfiguration;
import com.exemplo.clientesservice.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @BeforeEach
    void limpar() {
        clienteRepository.deleteAll().block();
    }

    @Test
    void deveSalvarEBuscarClientePorEmail() {
        var fluxo = clienteRepository.save(new Cliente("Ana Souza", "ana@teste.com"))
                .then(clienteRepository.findByEmail("ana@teste.com"));

        StepVerifier.create(fluxo)
                .assertNext(cliente -> {
                    assertThat(cliente.getId()).isNotNull();
                    assertThat(cliente.getNome()).isEqualTo("Ana Souza");
                })
                .verifyComplete();
    }

    @Test
    void deveInformarSeEmailExiste() {
        var fluxo = clienteRepository.save(new Cliente("Bruno", "bruno@teste.com"))
                .then(clienteRepository.existsByEmail("bruno@teste.com"))
                .zipWith(clienteRepository.existsByEmail("naoexiste@teste.com"));

        StepVerifier.create(fluxo)
                .assertNext(resultado -> {
                    assertThat(resultado.getT1()).isTrue();
                    assertThat(resultado.getT2()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void deveRespeitarEmailUnicoNoBanco() {
        var fluxo = clienteRepository.save(new Cliente("Carla", "carla@teste.com"))
                .then(clienteRepository.save(new Cliente("Carla 2", "carla@teste.com")));

        StepVerifier.create(fluxo)
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }
}
