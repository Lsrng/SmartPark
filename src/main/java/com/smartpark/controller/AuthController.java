package com.smartpark.controller;

import com.smartpark.common.result.Result;
import com.smartpark.pojo.dto.LoginDTO;
import com.smartpark.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户认证", description = "用户登录相关接口")
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(
            summary = "用户登录",
            description = "使用用户名和密码登录，返回JWT令牌"
    )
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("用户登录请求: username={}", loginDTO.getUsername());
        try {
            Map<String, Object> result = userService.login(loginDTO);
            return Result.success("登录成功", result);
        } catch (RuntimeException e) {
            log.warn("用户登录失败: username={}, reason={}", loginDTO.getUsername(), e.getMessage());
            return Result.error(401, e.getMessage());
        }
    }
}
