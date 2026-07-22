package com.smartpark.common.utils;

import io.jsonwebtoken.*;

import java.util.Date;
import java.util.Map;

/**
 * JWT(JSON Web Token)工具类
 * 提供JWT令牌的生成、解析和验证功能
 */
public class JwtUtils {

    /**
     * 生成JWT令牌
     *
     * @param secretKey 加密密钥，应与验证时使用的密钥一致
     * @param expiration 有效期（毫秒），从当前时间开始计算
     * @param claims 自定义声明内容，可以存储用户ID、角色等信息
     * @return 生成的JWT字符串
     */
    public static String generateToken(String secretKey, long expiration, Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)                      // 设置自定义声明(claims)
                .setIssuedAt(new Date())               // 设置签发时间(iat)
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 设置过期时间(exp)
                .signWith(SignatureAlgorithm.HS256, secretKey) // 使用HS256算法和密钥签名
                .compact();                            // 生成紧凑的URL安全字符串
    }

    /**
     * 解析JWT令牌
     *
     * @param token 待解析的JWT字符串
     * @param secretKey 加密密钥，必须与生成时使用的密钥一致
     * @return Claims对象，包含令牌中的所有声明(claims)
     */
    public static Claims parseToken(String token, String secretKey) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)              // 设置签名密钥
                    .parseClaimsJws(token)                 // 解析并验证令牌
                    .getBody();                           // 获取声明体
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("令牌已过期", e);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("不支持的令牌格式", e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("令牌格式错误", e);
        } catch (SignatureException e) {
            throw new RuntimeException("签名验证失败", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("非法参数", e);
        }
    }

    /**
     * 检查令牌是否过期
     *
     * @param token 待检查的JWT字符串
     * @param secretKey 加密密钥
     * @return true-已过期或无效，false-未过期
     *
     * 注意：此方法会捕获所有异常并返回true，适合用于简单验证
     * 如果需要区分不同异常类型，应直接调用parseToken方法
     */
    public static boolean isTokenExpired(String token, String secretKey) {
        try {
            Claims claims = parseToken(token, secretKey);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());      // 比较过期时间与当前时间
        } catch (Exception e) {
            return true;                              // 解析异常视为过期
        }
    }
}
