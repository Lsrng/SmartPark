package com.smartpark.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretKey;  // 加密密钥，建议长度至少32字符
    private Long expiration;   // token有效期（毫秒），如3600000（1小时）
    private String tokenName;  // 前端传递的token名称
}
