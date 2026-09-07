package com.core.coreboot.platform.customer.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.auth.config.CustomerAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class CustomerTokenService {
    private static final String CUSTOMER_AUDIENCE = "point-platform-customer-api";
    private static final String TOKEN_USE = "customer_access";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Duration MAXIMUM_TOKEN_TTL = Duration.ofHours(24);

    private final CustomerAuthProperties properties;
    private final Clock clock;

    @Autowired
    public CustomerTokenService(CustomerAuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    CustomerTokenService(CustomerAuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void assertConfigured() {
        algorithm();
        validatedTtl();
        validatedIssuer();
    }

    public IssuedCustomerToken issue(CustomerPrincipal principal) {
        Algorithm algorithm = algorithm();
        Duration ttl = validatedTtl();
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(ttl);
        String token = JWT.create()
                .withIssuer(validatedIssuer())
                .withAudience(CUSTOMER_AUDIENCE)
                .withSubject(String.valueOf(principal.getCustomerId()))
                .withClaim("token_use", TOKEN_USE)
                .withClaim("ver", principal.getTokenVersion())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
        return new IssuedCustomerToken(token, expiresAt, ttl.toSeconds());
    }

    public VerifiedCustomerToken verify(String token) {
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }
        try {
            JWTVerifier verifier = JWT.require(algorithm())
                    .withIssuer(validatedIssuer())
                    .withAudience(CUSTOMER_AUDIENCE)
                    .withClaim("token_use", TOKEN_USE)
                    .build();
            DecodedJWT decoded = verifier.verify(token);
            String subject = decoded.getSubject();
            if (subject == null || subject.isBlank()) {
                throw invalidToken();
            }
            Long customerId = Long.valueOf(subject);
            Integer tokenVersion = decoded.getClaim("ver").asInt();
            Instant issuedAt = decoded.getIssuedAtAsInstant();
            Instant expiresAt = decoded.getExpiresAtAsInstant();
            if (customerId <= 0
                    || tokenVersion == null
                    || issuedAt == null
                    || expiresAt == null
                    || decoded.getId() == null
                    || decoded.getId().isBlank()
                    || issuedAt.isAfter(clock.instant().plusSeconds(30))
                    || expiresAt.isAfter(issuedAt.plus(validatedTtl()).plusSeconds(30))) {
                throw invalidToken();
            }
            return new VerifiedCustomerToken(customerId, tokenVersion, expiresAt);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            throw invalidToken();
        }
    }

    private Algorithm algorithm() {
        String configuredSecret = properties.getJwtSecretBase64();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw notConfigured();
        }
        try {
            byte[] secret = Base64.getDecoder().decode(configuredSecret.trim());
            if (secret.length < MINIMUM_SECRET_BYTES) {
                throw notConfigured();
            }
            return Algorithm.HMAC256(secret);
        } catch (IllegalArgumentException ex) {
            throw notConfigured();
        }
    }

    private Duration validatedTtl() {
        Duration ttl = properties.getTokenTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAXIMUM_TOKEN_TTL) > 0) {
            throw notConfigured();
        }
        return ttl;
    }

    private String validatedIssuer() {
        String issuer = properties.getIssuer();
        if (issuer == null || issuer.isBlank()) {
            throw notConfigured();
        }
        return issuer.trim();
    }

    private CustomException invalidToken() {
        return new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_TOKEN_INVALID);
    }

    private CustomException notConfigured() {
        return new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_SECURITY_NOT_CONFIGURED);
    }

    public record IssuedCustomerToken(String token, Instant expiresAt, long expiresInSeconds) {
    }

    public record VerifiedCustomerToken(Long customerId, int tokenVersion, Instant expiresAt) {
    }
}
