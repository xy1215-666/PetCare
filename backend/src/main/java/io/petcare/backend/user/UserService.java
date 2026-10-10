package io.petcare.backend.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.petcare.backend.common.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CurrentUserVO getCurrentUser(long userId) {
        AppUser user = userMapper.selectOne(new QueryWrapper<AppUser>()
                .eq("id", userId)
                .eq("deleted", 0));
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("authenticated user is no longer active");
        }
        return new CurrentUserVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreateTime());
    }
}
