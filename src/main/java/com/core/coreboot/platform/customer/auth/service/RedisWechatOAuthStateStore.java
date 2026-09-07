package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.auth.config.WechatH5SettingsProvider;
import com.core.coreboot.platform.customer.auth.model.WechatOAuthState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class RedisWechatOAuthStateStore implements WechatOAuthStateStore {
    private static final String KEY_PREFIX = "point-platform:wechat-h5-oauth-state:";
    private static final String DEFAULT_RETURN_PATH = "/pages/index/index";
    private static final int STATE_BYTES = 32;
    private static final int MAX_ISSUE_ATTEMPTS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if value then
                redis.call('DEL', KEYS[1])
            end
            return value
            """, String.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WechatH5SettingsProvider settingsProvider;

    @Override
    public String issue(String returnPath) {
        WechatOAuthState payload = new WechatOAuthState(normalizeReturnPath(returnPath));
        String json = writePayload(payload);
        Duration stateTtl = settingsProvider.requireValid().stateTtl();
        try {
            for (int attempt = 0; attempt < MAX_ISSUE_ATTEMPTS; attempt++) {
                String state = newState();
                Boolean stored = redisTemplate.opsForValue().setIfAbsent(
                        key(state),
                        json,
                        stateTtl
                );
                if (Boolean.TRUE.equals(stored)) {
                    return state;
                }
            }
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        }
        throw new CustomException(ExceptionEnum.SYSTEM_ERROR);
    }

    @Override
    public WechatOAuthState consume(String state) {
        if (!isStateFormatValid(state)) {
            throw invalidState();
        }
        try {
            String json = redisTemplate.execute(CONSUME_SCRIPT, List.of(key(state)));
            if (json == null || json.isBlank()) {
                throw invalidState();
            }
            WechatOAuthState payload = objectMapper.readValue(json, WechatOAuthState.class);
            if (payload.returnPath() == null || payload.returnPath().isBlank()) {
                throw invalidState();
            }
            return payload;
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        } catch (JsonProcessingException ex) {
            throw invalidState();
        }
    }

    private String writePayload(WechatOAuthState payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ExceptionEnum.SYSTEM_ERROR);
        }
    }

    private String newState() {
        byte[] bytes = new byte[STATE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String key(String state) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(state.getBytes(StandardCharsets.US_ASCII));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private boolean isStateFormatValid(String state) {
        return state != null && state.matches("[A-Za-z0-9_-]{43}");
    }

    private String normalizeReturnPath(String returnPath) {
        return DEFAULT_RETURN_PATH.equals(returnPath) ? returnPath : DEFAULT_RETURN_PATH;
    }

    private CustomException invalidState() {
        return new CustomException(ExceptionEnum.PLATFORM_WECHAT_AUTH_STATE_INVALID);
    }
}
