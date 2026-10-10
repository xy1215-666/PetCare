package io.petcare.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Size(max = 64, message = "username is too long")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 12, max = 72, message = "password must contain 12 to 72 characters")
        String password,

        @Email(message = "email must be valid")
        @Size(max = 254, message = "email is too long")
        String email,

        @Size(max = 32, message = "phone is too long")
        String phone) {

    public RegisterRequest {
        if (username != null) {
            username = username.trim();
        }
        if (email != null) {
            email = email.trim();
        }
        if (phone != null) {
            phone = phone.trim();
        }
    }
}
