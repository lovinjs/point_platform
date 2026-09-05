package com.core.coreboot.platform.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.auth.config.AdminSecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class AdminTokenService {
    private static final String ADMIN_AUDIENCE = "point-platform-admin-api";
    private static final String TOKEN_USE = "admin_access";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Duration MAXIMUM_TOKEN_TTL = Duration.ofHours(24);

    private final AdminSecurityProperties properties;
    private final Clock clock;

    @Autowired
    public AdminTokenService(AdminSecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AdminTokenService(AdminSecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedAdminToken issue(AdminUserPrincipal principal) {
        Algorithm algorithm = algorithm();
        Duration ttl = validatedTtl();
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(ttl);
        String token = JWT.create()
                .withIssuer(validatedIssuer())
                .withAudience(ADMIN_AUDIENCE)
                .withSubject(String.valueOf(principal.getUserId()))
                .withClaim("username", principal.getUsername())
                .withClaim("token_use", TOKEN_USE)
                .withClaim("ver", principal.getTokenVersion())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
        return new IssuedAdminToken(token, expiresAt, ttl.toSeconds());
    }

    public VerifiedAdminToken verify(String token) {
        if (token == null || token.isBlank()) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
        }
        try {
            JWTVerifier verifier = JWT.require(algorithm())
                    .withIssuer(validatedIssuer())
                    .withAudience(ADMIN_AUDIENCE)
                    .withClaim("token_use", TOKEN_USE)
                    .build();
            DecodedJWT decoded = verifier.verify(token);
            Long userId = Long.valueOf(decoded.getSubject());
            Integer tokenVersion = decoded.getClaim("ver").asInt();
            Instant issuedAt = decoded.getIssuedAtAsInstant();
            Instant expiresAt = decoded.getExpiresAtAsInstant();
            if (tokenVersion == null
                    || issuedAt == null
                    || expiresAt == null
                    || decoded.getId() == null
                    || decoded.getId().isBlank()
                    || issuedAt.isAfter(clock.instant().plusSeconds(30))
                    || expiresAt.isAfter(issuedAt.plus(validatedTtl()).plusSeconds(30))) {
                throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
            }
            return new VerifiedAdminToken(userId, tokenVersion, expiresAt);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
        }
    }

    private Algorithm algorithm() {
        String configuredSecret = properties.getJwtSecretBase64();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED);
        }
        try {
            byte[] secret = Base64.getDecoder().decode(configuredSecret.trim());
            if (secret.length < MINIMUM_SECRET_BYTES) {
                throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED);
            }
            return Algorithm.HMAC256(secret);
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED);
        }
    }

    private Duration validatedTtl() {
        Duration ttl = properties.getTokenTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAXIMUM_TOKEN_TTL) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED);
        }
        return ttl;
    }

    private String validatedIssuer() {
        String issuer = properties.getIssuer();
        if (issuer == null || issuer.isBlank()) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED);
        }
        return issuer.trim();
    }

    public record IssuedAdminToken(String token, Instant expiresAt, long expiresInSeconds) {
    }

    public record VerifiedAdminToken(Long userId, int tokenVersion, Instant expiresAt) {
    }
}
