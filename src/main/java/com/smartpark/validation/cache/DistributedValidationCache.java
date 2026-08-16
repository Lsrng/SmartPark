package com.smartpark.validation.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.pojo.entity.ValidationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DistributedValidationCache {

    private static final String KEY_PREFIX = "validation_config:";
    private static final long EXPIRE_HOURS = 24;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public DistributedValidationCache(RedisTemplate<String, String> redisTemplate,
                                       ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ValidationConfig> getConfigs(Long typeId, Integer version) {
        String key = buildKey(typeId, version);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json,
                        new TypeReference<List<ValidationConfig>>() {});
            }
        } catch (JsonProcessingException e) {
            log.error("解析校验配置缓存失败, key={}", key, e);
        }
        return Collections.emptyList();
    }

    public void setConfigs(Long typeId, Integer version, List<ValidationConfig> configs) {
        String key = buildKey(typeId, version);
        try {
            String json = objectMapper.writeValueAsString(configs);
            redisTemplate.opsForValue().set(key, json, EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("序列化校验配置缓存失败, key={}", key, e);
        }
    }

    public void evictAllCache(Long typeId) {
        String pattern = KEY_PREFIX + typeId + ":*";
        var cursor = redisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(100).build());
        List<String> keys = new ArrayList<>();
        while (cursor.hasNext()) {
            keys.add(cursor.next());
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("已清理 {} 个校验配置缓存, typeId={}", keys.size(), typeId);
        }
    }

    private String buildKey(Long typeId, Integer version) {
        return KEY_PREFIX + typeId + ":" + version;
    }
}
