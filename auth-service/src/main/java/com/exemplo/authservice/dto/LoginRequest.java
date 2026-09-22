package com.exemplo.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email e obrigatorio")
        String email,

        @NotBlank(message = "senha e obrigatoria")
        String senha) {
}
