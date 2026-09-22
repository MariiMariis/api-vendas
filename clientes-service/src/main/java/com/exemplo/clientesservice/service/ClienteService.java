package com.exemplo.clientesservice.service;

import com.exemplo.clientesservice.model.Cliente;
import com.exemplo.clientesservice.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Regra de negocio de Cliente. O controller nao fala direto com o repository,
 * fala com este service.
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Flux<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Mono<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    public Mono<Cliente> criar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank()
                || cliente.getEmail() == null || !cliente.getEmail().contains("@")) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "nome e email valido sao obrigatorios"));
        }
        cliente.setId(null);
        cliente.setEmail(cliente.getEmail().trim().toLowerCase());
        return clienteRepository.existsByEmail(cliente.getEmail())
                .flatMap(existe -> existe
                        ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                                "Ja existe um cliente cadastrado com o email " + cliente.getEmail()))
                        : clienteRepository.save(cliente));
    }
}
