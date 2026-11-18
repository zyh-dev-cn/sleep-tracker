package com.zyh.sleep.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyh.sleep.entity.User;
import com.zyh.sleep.mapper.UserMapper;
import com.zyh.sleep.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public String register(String username, String password) {
        if (username == null || username.trim().length() < 3 || username.trim().length() > 20) {
            throw new IllegalArgumentException("用户名需要 3-20 个字符");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        String name = username.trim();
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, name));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已被注册");
        }
        User user = new User();
        user.setUsername(name);
        user.setPassword(passwordEncoder.encode(password));
        userMapper.insert(user);
        return jwtUtil.generate(user.getId());
    }

    public String login(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return jwtUtil.generate(user.getId());
    }
}
