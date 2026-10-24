package com.ailearning.activity_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Resilient Redis cache service.
 * Operates gracefully even if Redis is temporarily unreachable.
 */
@Slf4j
@Service
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;

    public RedisCacheService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String get(String key) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis GET failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void set(String key, String value, long ttlSeconds) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
            log.info("Cached key [{}] in Redis for {}s", key, ttlSeconds);
        } catch (Exception e) {
            log.warn("Redis SET failed for key {}: {}", key, e.getMessage());
        }
    }

    public void delete(String key) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(key);
            log.info("Evicted key [{}] from Redis cache", key);
        } catch (Exception e) {
            log.warn("Redis DELETE failed for key {}: {}", key, e.getMessage());
        }
    }

    public void deletePattern(String pattern) {
        if (redisTemplate == null) {
            return;
        }
        try {
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} keys matching pattern [{}] from Redis cache", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Redis deletePattern failed for pattern {}: {}", pattern, e.getMessage());
        }
    }

    public boolean isConnected() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            var connection = redisTemplate.getConnectionFactory() != null
                    ? redisTemplate.getConnectionFactory().getConnection()
                    : null;
            return connection != null && "PONG".equalsIgnoreCase(connection.ping());
        } catch (Exception e) {
            return false;
        }
    }
}
