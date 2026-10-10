package io.petcare.backend.user;

import java.time.LocalDateTime;

public record CurrentUserVO(
        long id,
        String username,
        String email,
        String phone,
        String role,
        LocalDateTime createTime) {
}
