package io.petcare.backend.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, RegisteredUserVO user) {
}
