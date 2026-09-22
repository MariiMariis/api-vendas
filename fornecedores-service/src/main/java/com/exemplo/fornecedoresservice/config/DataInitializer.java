package com.exemplo.fornecedoresservice.config;

import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final FornecedorRepository fornecedorRepository;

    public DataInitializer(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @Override
    public void run(String... args) {
        fornecedorRepository.save(new Fornecedor("Alfa Distribuidora", "11222333000181"));
        fornecedorRepository.save(new Fornecedor("Beta Suprimentos", "22333444000172"));
        fornecedorRepository.save(new Fornecedor("Gama Atacado", "33444555000163"));
        fornecedorRepository.save(new Fornecedor("Delta Insumos", "44555666000154"));
        fornecedorRepository.save(new Fornecedor("Omega Materiais", "55666777000145"));
    }
}
