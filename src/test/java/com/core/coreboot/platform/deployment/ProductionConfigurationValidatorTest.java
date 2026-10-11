package com.core.coreboot.platform.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionConfigurationValidatorTest {
    @Test
    void shouldAcceptCompletePrivateServerConfiguration() {
        assertDoesNotThrow(() -> validator(validEnvironment()).afterPropertiesSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
            "spring.data.redis.host", "spring.data.redis.password",
            "platform.security.admin.jwt-secret-base64", "platform.security.customer.jwt-secret-base64"
    })
    void shouldRejectMissingRequiredValues(String property) {
        MockEnvironment environment = validEnvironment().withProperty(property, " ");
        assertThrows(IllegalStateException.class, () -> validator(environment).afterPropertiesSet());
    }

    @Test
    void shouldRejectRootAccount() {
        MockEnvironment environment = validEnvironment().withProperty("spring.datasource.username", "ROOT");
        assertThrows(IllegalStateException.class, () -> validator(environment).afterPropertiesSet());
    }

    @Test
    void shouldRejectSqlLoggingDriverUrlWithoutLeakingCredentials() {
        String url = "jdbc:p6spy:mysql://test/db?password=must-not-appear";
        MockEnvironment environment = validEnvironment().withProperty("spring.datasource.url", url);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> validator(environment).afterPropertiesSet());
        assertFalse(error.getMessage().contains(url));
        assertFalse(error.getMessage().contains("must-not-appear"));
    }

    @Test
    void shouldRejectUnresolvedPlaceholder() {
        MockEnvironment environment = validEnvironment().withProperty("spring.data.redis.host", "${UNSET_HOST}");
        assertThrows(IllegalArgumentException.class, () -> validator(environment).afterPropertiesSet());
    }

    @Test
    void shouldRejectMalformedJwtSecretWithoutLeakingIt() {
        MockEnvironment environment = validEnvironment()
                .withProperty("platform.security.admin.jwt-secret-base64", "invalid-secret!");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> validator(environment).afterPropertiesSet());
        assertFalse(error.getMessage().contains("invalid-secret!"));
    }

    @Test
    void shouldRejectShortJwtSecret() {
        MockEnvironment environment = validEnvironment()
                .withProperty("platform.security.customer.jwt-secret-base64", encodedSecret(16, (byte) 3));
        assertThrows(IllegalStateException.class, () -> validator(environment).afterPropertiesSet());
    }

    @Test
    void shouldRejectSharedJwtSecretsEvenWithWhitespace() {
        MockEnvironment environment = validEnvironment().withProperty(
                "platform.security.customer.jwt-secret-base64", " " + encodedSecret(32, (byte) 1) + " ");
        assertThrows(IllegalStateException.class, () -> validator(environment).afterPropertiesSet());
    }

    @Test
    void shouldRejectSmsLogMode() {
        MockEnvironment environment = validEnvironment().withProperty("platform.phone-verification.mode", "LOG");
        assertThrows(IllegalStateException.class, () -> validator(environment).afterPropertiesSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spring.flyway.enabled", "spring.flyway.clean-disabled", "spring.flyway.validate-on-migrate"
    })
    void shouldRejectDisabledMigrationSafety(String property) {
        MockEnvironment environment = validEnvironment().withProperty(property, "false");
        assertThrows(IllegalStateException.class, () -> validator(environment).afterPropertiesSet());
    }

    @Test
    void shouldRejectAutomaticBaseline() {
        MockEnvironment environment = validEnvironment().withProperty("spring.flyway.baseline-on-migrate", "true");
        assertThrows(IllegalStateException.class, () -> validator(environment).afterPropertiesSet());
    }

    private ProductionConfigurationValidator validator(MockEnvironment environment) {
        return new ProductionConfigurationValidator(environment);
    }

    private MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://mysql:3306/point_platform?sslMode=REQUIRED")
                .withProperty("spring.datasource.username", "point_platform_app")
                .withProperty("spring.datasource.password", "unit-test-db-password")
                .withProperty("spring.data.redis.host", "redis")
                .withProperty("spring.data.redis.password", "unit-test-redis-password")
                .withProperty("platform.security.admin.jwt-secret-base64", encodedSecret(32, (byte) 1))
                .withProperty("platform.security.customer.jwt-secret-base64", encodedSecret(32, (byte) 2));
    }

    private String encodedSecret(int length, byte value) {
        byte[] secret = new byte[length];
        Arrays.fill(secret, value);
        return Base64.getEncoder().encodeToString(secret);
    }
}
