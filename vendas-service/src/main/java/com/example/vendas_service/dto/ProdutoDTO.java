package com.example.vendas_service.dto;

import java.math.BigDecimal;

public record ProdutoDTO(Long id, String nome, BigDecimal preco) {
}
