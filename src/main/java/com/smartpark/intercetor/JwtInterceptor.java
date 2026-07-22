package com.smartpark.intercetor;

import com.smartpark.common.context.BaseContext;
import com.smartpark.common.properties.JwtProperties;
import com.smartpark.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录认证拦截器（改进版）
 * 作用：
 *  1. 拦截需要登录的请求
 *  2. 从请求头中读取 JWT Token
 *  3. 校验 Token 是否有效
 *  4. 如果有效，将用户信息保存到 UserContext（ThreadLocal）
 *  5. 请求结束后自动清理上下文，防止内存泄漏
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties; // 从配置文件读取的 JWT 配置信息

    /**
     * 在请求处理前执行（Controller 方法调用前）
     * 用于校验 Token 并保存当前用户信息
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. 放行 OPTIONS 预检请求（主要用于跨域请求的预检，不能拦截）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("[JwtInterceptor]-request.getMethod(): {}", request.getMethod());
            return true;
        }

        // 2. 从请求头中获取 Token
        String token = request.getHeader(jwtProperties.getTokenName());
        if (token == null || token.trim().isEmpty()) {
            // 如果没有 Token，则返回 401 未授权
            sendUnauthorized(response, "未提供Token");
            return false; // 拦截请求
        }

        try {
            // 3. 解析 Token（验证签名、检查过期时间等）
            Claims claims = JwtUtils.parseToken(token, jwtProperties.getSecretKey());

            // 4. 解析成功，将用户信息存入 BaseContext（ThreadLocal）
            BaseContext.setCurrentId(Long.valueOf(claims.get("id").toString()));

            // 5. 校验成功，放行请求
            return true;
        } catch (RuntimeException e) {
            // 如果解析失败（Token 无效、过期等），返回 401
            sendUnauthorized(response, "Token无效或已过期");
            return false;
        }
    }

    /**
     * 在请求完成后执行（视图渲染后）
     * 用于清理 ThreadLocal 中的用户信息，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId(); // 清除用户上下文
    }

    /**
     * 返回 401 未授权的 JSON 响应
     * @param response 响应对象
     * @param message  错误提示信息
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 状态码 401
        response.setContentType("application/json;charset=UTF-8"); // 响应类型为 JSON，编码 UTF-8
        // 返回 JSON 格式的错误信息
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}