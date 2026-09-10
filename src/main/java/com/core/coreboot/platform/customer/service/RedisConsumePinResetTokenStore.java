package com.core.coreboot.platform.customer.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisConsumePinResetTokenStore implements ConsumePinResetTokenStore {
    private static final String KEY_PREFIX = "point-platform:consume-pin-reset:";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[1] then
                return 0
            end
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public String issue(Long customerId, Duration ttl) {
        if (customerId == null || customerId <= 0 || ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_PIN_CONFIGURATION_INVALID);
        }
        String token = newToken();
        try {
            redisTemplate.opsForValue().set(key(customerId), hash(token), ttl);
            return token;
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }

    @Override
    public void consume(Long customerId, String resetToken) {
        if (customerId == null || customerId <= 0 || !isTokenFormatValid(resetToken)) {
            throw invalidToken();
        }
        try {
            Long consumed = redisTemplate.execute(
                    CONSUME_SCRIPT,
                    List.of(key(customerId)),
                    hash(resetToken)
            );
            if (!Long.valueOf(1L).equals(consumed)) {
                throw invalidToken();
            }
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String key(Long customerId) {
        return KEY_PREFIX + customerId;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private boolean isTokenFormatValid(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }

    private CustomException invalidToken() {
        return new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_RESET_TOKEN_INVALID);
    }
}
