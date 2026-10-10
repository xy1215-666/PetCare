package io.petcare.backend.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.petcare.backend.common.exception.BadRequestException;
import io.petcare.backend.common.exception.ConflictException;
import io.petcare.backend.common.exception.UnauthorizedException;
import io.petcare.backend.user.AppUser;
import io.petcare.backend.user.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private static final String DUPLICATE_MESSAGE = "username, email, or phone is already in use";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public RegisteredUserVO register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (username.length() < 4 || username.length() > 32 || !username.matches("[a-z0-9_]+")) {
            throw new BadRequestException("username must be 4 to 32 ASCII letters, digits, or underscores");
        }

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("password must not exceed 72 UTF-8 bytes");
        }

        String email = normalizeOptional(request.email());
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }
        String phone = normalizeOptional(request.phone());

        if (exists("username", username)
                || (email != null && exists("email", email))
                || (phone != null && exists("phone", phone))) {
            throw new ConflictException(DUPLICATE_MESSAGE);
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setDeleted(0);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // The database unique keys remain the final guard against concurrent registrations.
            throw new ConflictException(DUPLICATE_MESSAGE);
        }

        return new RegisteredUserVO(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(), user.getRole());
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        AppUser user = userMapper.selectOne(new QueryWrapper<AppUser>()
                .eq("username", username)
                .eq("deleted", 0));
        if (user == null || !"ACTIVE".equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("username or password is incorrect");
        }
        return jwtTokenService.issue(user);
    }

    private boolean exists(String column, String value) {
        return userMapper.selectCount(new QueryWrapper<AppUser>().eq(column, value)) > 0;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
