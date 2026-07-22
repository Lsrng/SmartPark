package com.smartpark.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域请求
 */
@Configuration
@AllArgsConstructor
public class CorsConfig {
    /**
     * 配置跨域过滤器Bean
     * 用于处理跨域请求（CORS）
     *
     * @return CorsFilter 跨域过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建基于URL的CORS配置源
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 创建CORS配置对象
        final CorsConfiguration corsConfiguration = new CorsConfiguration();

        // 允许发送身份凭证（如cookies）
        corsConfiguration.setAllowCredentials(true);

        // 允许所有请求头
        corsConfiguration.addAllowedHeader("*");

        // 允许所有来源模式（使用模式比直接使用"*"更灵活）
        corsConfiguration.addAllowedOriginPattern("*");

        // 允许所有HTTP方法（GET, POST, PUT等）
        corsConfiguration.addAllowedMethod("*");

        // 为所有路径（/**）注册CORS配置
        source.registerCorsConfiguration("/**", corsConfiguration);

        // 创建并返回CORS过滤器
        return new CorsFilter(source);
    }
}
