package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.auth.config.CustomerAuthProperties;
import com.core.coreboot.platform.customer.auth.model.CustomerLoginTicket;
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
public class RedisCustomerLoginTicketStore implements CustomerLoginTicketStore {
    private static final String KEY_PREFIX = "point-platform:customer-login-ticket:";
    private static final String DEFAULT_RETURN_PATH = "/pages/index/index";
    private static final int TICKET_BYTES = 32;
    private static final int MAX_ISSUE_ATTEMPTS = 3;
    private static final Duration MAXIMUM_TICKET_TTL = Duration.ofMinutes(5);
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
    private final CustomerAuthProperties properties;

    @Override
    public String issue(Long customerId, String returnPath) {
        if (customerId == null || customerId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        Duration ttl = validatedTicketTtl();
        String payload = writePayload(new CustomerLoginTicket(customerId, normalizeReturnPath(returnPath)));
        try {
            for (int attempt = 0; attempt < MAX_ISSUE_ATTEMPTS; attempt++) {
                String ticket = newTicket();
                Boolean stored = redisTemplate.opsForValue()
                        .setIfAbsent(key(ticket), payload, ttl);
                if (Boolean.TRUE.equals(stored)) {
                    return ticket;
                }
            }
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        }
        throw new CustomException(ExceptionEnum.SYSTEM_ERROR);
    }

    @Override
    public CustomerLoginTicket consume(String ticket) {
        if (!isTicketFormatValid(ticket)) {
            throw invalidTicket();
        }
        try {
            String payload = redisTemplate.execute(CONSUME_SCRIPT, List.of(key(ticket)));
            if (payload == null || payload.isBlank()) {
                throw invalidTicket();
            }
            CustomerLoginTicket loginTicket = objectMapper.readValue(payload, CustomerLoginTicket.class);
            if (loginTicket.customerId() == null || loginTicket.customerId() <= 0) {
                throw invalidTicket();
            }
            return loginTicket;
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        } catch (JsonProcessingException ex) {
            throw invalidTicket();
        }
    }

    private Duration validatedTicketTtl() {
        Duration ttl = properties.getLoginTicketTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAXIMUM_TICKET_TTL) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_SECURITY_NOT_CONFIGURED);
        }
        return ttl;
    }

    private String writePayload(CustomerLoginTicket payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new CustomException(ExceptionEnum.SYSTEM_ERROR);
        }
    }

    private String newTicket() {
        byte[] bytes = new byte[TICKET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String key(String ticket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ticket.getBytes(StandardCharsets.US_ASCII));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private boolean isTicketFormatValid(String ticket) {
        return ticket != null && ticket.matches("[A-Za-z0-9_-]{43}");
    }

    private String normalizeReturnPath(String returnPath) {
        return DEFAULT_RETURN_PATH.equals(returnPath) ? returnPath : DEFAULT_RETURN_PATH;
    }

    private CustomException invalidTicket() {
        return new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_LOGIN_TICKET_INVALID);
    }
}
