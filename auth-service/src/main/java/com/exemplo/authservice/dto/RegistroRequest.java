package com.exemplo.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "nome e obrigatorio")
        String nome,

        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email invalido")
        String email,

        @NotBlank(message = "senha e obrigatoria")
        @Size(min = 6, message = "senha deve ter no minimo 6 caracteres")
        String senha) {
}
