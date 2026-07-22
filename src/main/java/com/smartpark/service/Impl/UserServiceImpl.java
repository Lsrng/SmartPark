package com.smartpark.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartpark.common.properties.JwtProperties;
import com.smartpark.common.utils.JwtUtils;
import com.smartpark.mapper.UserMapper;
import com.smartpark.pojo.dto.LoginDTO;
import com.smartpark.pojo.entity.User;
import com.smartpark.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtProperties jwtProperties;

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(queryWrapper);

        // 2. 校验用户是否存在
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 校验密码
        if (!loginDTO.getPassword().equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        String token = JwtUtils.generateToken(
                jwtProperties.getSecretKey(),
                jwtProperties.getExpiration(),
                claims
        );

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenName", jwtProperties.getTokenName());
        result.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername()
        ));

        log.info("用户 [{}] 登录成功，token已生成", user.getUsername());
        return result;
    }
}
