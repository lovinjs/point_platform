package com.core.coreboot.platform.customer.auth.security;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.auth.config.CustomerAuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerTokenServiceTest {

    @Test
    void shouldIssueAndVerifyCustomerToken() {
        CustomerTokenService service = new CustomerTokenService(validProperties((byte) 7));

        CustomerTokenService.IssuedCustomerToken issued = service.issue(principal(4));
        CustomerTokenService.VerifiedCustomerToken verified = service.verify(issued.token());

        assertFalse(issued.token().isBlank());
        assertEquals(2 * 60 * 60L, issued.expiresInSeconds());
        assertEquals(12L, verified.customerId());
        assertEquals(4, verified.tokenVersion());
    }

    @Test
    void shouldRejectTokenSignedByAnotherCustomerSecret() {
        CustomerTokenService issuingService = new CustomerTokenService(validProperties((byte) 7));
        CustomerTokenService verifyingService = new CustomerTokenService(validProperties((byte) 8));
        String token = issuingService.issue(principal(0)).token();

        CustomException exception = assertThrows(CustomException.class, () -> verifyingService.verify(token));

        assertEquals(ExceptionEnum.PLATFORM_CUSTOMER_TOKEN_INVALID.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectMissingOrShortSecret() {
        CustomerAuthProperties missing = new CustomerAuthProperties();
        CustomerAuthProperties shortSecret = validProperties((byte) 1);
        shortSecret.setJwtSecretBase64(Base64.getEncoder().encodeToString(new byte[16]));

        CustomException missingException = assertThrows(
                CustomException.class,
                () -> new CustomerTokenService(missing).issue(principal(0))
        );
        CustomException shortException = assertThrows(
                CustomException.class,
                () -> new CustomerTokenService(shortSecret).issue(principal(0))
        );

        assertEquals(ExceptionEnum.PLATFORM_CUSTOMER_SECURITY_NOT_CONFIGURED.getCode(),
                missingException.getCode());
        assertEquals(ExceptionEnum.PLATFORM_CUSTOMER_SECURITY_NOT_CONFIGURED.getCode(),
                shortException.getCode());
    }

    private CustomerAuthProperties validProperties(byte fill) {
        byte[] secret = new byte[32];
        java.util.Arrays.fill(secret, fill);
        CustomerAuthProperties properties = new CustomerAuthProperties();
        properties.setJwtSecretBase64(Base64.getEncoder().encodeToString(secret));
        properties.setTokenTtl(Duration.ofHours(2));
        properties.setIssuer("point-platform-customer-test");
        return properties;
    }

    private CustomerPrincipal principal(int tokenVersion) {
        return new CustomerPrincipal(12L, tokenVersion, CustomerStatus.ACTIVE);
    }
}
