package com.exemplo.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken e obrigatorio")
        String refreshToken) {
}
