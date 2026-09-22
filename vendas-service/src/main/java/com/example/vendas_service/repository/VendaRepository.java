package com.example.vendas_service.repository;

import com.example.vendas_service.models.Venda;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface VendaRepository extends ReactiveCrudRepository<Venda, Long> {

    Flux<Venda> findByUsuarioOrderByDataVendaDesc(String usuario);

    Flux<Venda> findByIdProduto(Long idProduto);
}
