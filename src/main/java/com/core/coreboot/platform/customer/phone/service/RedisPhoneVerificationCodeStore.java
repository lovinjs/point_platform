package com.core.coreboot.platform.customer.phone.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.phone.config.PhoneVerificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisPhoneVerificationCodeStore implements PhoneVerificationCodeStore {
    private static final String KEY_PREFIX = "point-platform:phone-verification:";
    private static final Duration HOUR_WINDOW = Duration.ofHours(1);
    private static final Duration DAY_WINDOW = Duration.ofHours(24);

    private static final DefaultRedisScript<Long> RESERVE_SEND_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return -1
            end

            for index = 2, 6 do
                local current = tonumber(redis.call('GET', KEYS[index]) or '0')
                local limit = tonumber(ARGV[index + 2])
                if current >= limit then
                    return index
                end
            end

            redis.call('SET', KEYS[1], '1', 'PX', ARGV[1])
            for index = 2, 6 do
                local nextValue = redis.call('INCR', KEYS[index])
                if nextValue == 1 then
                    local ttl = ARGV[3]
                    if index == 2 or index == 4 then
                        ttl = ARGV[2]
                    end
                    redis.call('PEXPIRE', KEYS[index], ttl)
                end
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> CONSUME_MATCHED_CODE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[1] then
                return 0
            end
            redis.call('DEL', KEYS[1])
            redis.call('DEL', KEYS[2])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[1] then
                return -1
            end

            local attempts = redis.call('INCR', KEYS[2])
            if attempts == 1 then
                local codeTtl = redis.call('PTTL', KEYS[1])
                if codeTtl > 0 then
                    redis.call('PEXPIRE', KEYS[2], codeTtl)
                end
            end
            if attempts >= tonumber(ARGV[2]) then
                redis.call('DEL', KEYS[1])
                redis.call('DEL', KEYS[2])
            end
            return attempts
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void reserveSend(Long customerId, String phone, PhoneVerificationProperties properties) {
        String phoneHash = hash(phone);
        List<String> keys = List.of(
                KEY_PREFIX + "cooldown:" + customerId + ":" + phoneHash,
                KEY_PREFIX + "customer-hour:" + customerId,
                KEY_PREFIX + "customer-day:" + customerId,
                KEY_PREFIX + "phone-hour:" + phoneHash,
                KEY_PREFIX + "phone-day:" + phoneHash,
                KEY_PREFIX + "global-day"
        );
        try {
            Long result = redisTemplate.execute(
                    RESERVE_SEND_SCRIPT,
                    keys,
                    String.valueOf(properties.getResendInterval().toMillis()),
                    String.valueOf(HOUR_WINDOW.toMillis()),
                    String.valueOf(DAY_WINDOW.toMillis()),
                    String.valueOf(properties.getCustomerHourlyLimit()),
                    String.valueOf(properties.getCustomerDailyLimit()),
                    String.valueOf(properties.getPhoneHourlyLimit()),
                    String.valueOf(properties.getPhoneDailyLimit()),
                    String.valueOf(properties.getGlobalDailyLimit())
            );
            if (result == null) {
                throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
            }
            if (result == -1L) {
                throw new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_TOO_FREQUENT);
            }
            if (result > 0L) {
                throw new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_LIMIT_REACHED);
            }
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }

    @Override
    public void saveCode(Long customerId, String phone, String verificationCode, Duration ttl) {
        String codeKey = codeKey(customerId, phone);
        try {
            redisTemplate.opsForValue().set(codeKey, passwordEncoder.encode(verificationCode), ttl);
            redisTemplate.delete(attemptKey(customerId, phone));
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }

    @Override
    public void verifyAndConsume(Long customerId, String phone, String verificationCode, int maxAttempts) {
        String codeKey = codeKey(customerId, phone);
        String attemptsKey = attemptKey(customerId, phone);
        try {
            String encodedCode = redisTemplate.opsForValue().get(codeKey);
            if (encodedCode == null || encodedCode.isBlank()) {
                throw invalidCode();
            }

            if (passwordEncoder.matches(verificationCode, encodedCode)) {
                Long consumed = redisTemplate.execute(
                        CONSUME_MATCHED_CODE_SCRIPT,
                        List.of(codeKey, attemptsKey),
                        encodedCode
                );
                if (!Long.valueOf(1L).equals(consumed)) {
                    throw invalidCode();
                }
                return;
            }

            Long failedAttempts = redisTemplate.execute(
                    RECORD_FAILURE_SCRIPT,
                    List.of(codeKey, attemptsKey),
                    encodedCode,
                    String.valueOf(maxAttempts)
            );
            if (failedAttempts != null && failedAttempts >= maxAttempts) {
                throw new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_LOCKED);
            }
            throw invalidCode();
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }

    @Override
    public void invalidate(Long customerId, String phone) {
        try {
            redisTemplate.delete(List.of(
                    codeKey(customerId, phone),
                    attemptKey(customerId, phone)
            ));
        } catch (DataAccessException ex) {
            throw new CustomException(ExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }

    private String codeKey(Long customerId, String phone) {
        return KEY_PREFIX + "code:" + customerId + ":" + hash(phone);
    }

    private String attemptKey(Long customerId, String phone) {
        return KEY_PREFIX + "attempt:" + customerId + ":" + hash(phone);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private CustomException invalidCode() {
        return new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_INVALID);
    }
}
