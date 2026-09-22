package com.exemplo.clientesservice.repository;

import com.exemplo.clientesservice.model.Cliente;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ClienteRepository extends ReactiveCrudRepository<Cliente, Long> {

    Mono<Cliente> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);
}
