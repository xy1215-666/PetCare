package io.petcare.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "username is required")
        @Size(max = 64, message = "username is too long")
        String username,

        @NotBlank(message = "password is required")
        @Size(max = 72, message = "password is too long")
        String password) {

    public LoginRequest {
        if (username != null) {
            username = username.trim();
        }
    }
}
