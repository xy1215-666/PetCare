package io.petcare.backend.auth;

public record RegisteredUserVO(long id, String username, String email, String phone, String role) {
}
