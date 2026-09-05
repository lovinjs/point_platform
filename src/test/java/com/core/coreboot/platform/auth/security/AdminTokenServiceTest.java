package com.core.coreboot.platform.auth.security;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.auth.config.AdminSecurityProperties;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminTokenServiceTest {

    @Test
    void shouldIssueAndVerifyAdminToken() {
        AdminSecurityProperties properties = validProperties((byte) 7);
        AdminTokenService service = new AdminTokenService(properties);
        AdminUserPrincipal principal = principal(3);

        AdminTokenService.IssuedAdminToken issued = service.issue(principal);
        AdminTokenService.VerifiedAdminToken verified = service.verify(issued.token());

        assertFalse(issued.token().isBlank());
        assertEquals(8 * 60 * 60L, issued.expiresInSeconds());
        assertEquals(10L, verified.userId());
        assertEquals(3, verified.tokenVersion());
    }

    @Test
    void shouldRejectTokenSignedByAnotherSecret() {
        AdminTokenService issuingService = new AdminTokenService(validProperties((byte) 7));
        AdminTokenService verifyingService = new AdminTokenService(validProperties((byte) 8));
        String token = issuingService.issue(principal(0)).token();

        CustomException exception = assertThrows(CustomException.class, () -> verifyingService.verify(token));

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectMissingOrShortSecret() {
        AdminSecurityProperties missing = new AdminSecurityProperties();
        AdminSecurityProperties shortSecret = validProperties((byte) 1);
        shortSecret.setJwtSecretBase64(Base64.getEncoder().encodeToString(new byte[16]));

        CustomException missingException = assertThrows(
                CustomException.class,
                () -> new AdminTokenService(missing).issue(principal(0))
        );
        CustomException shortException = assertThrows(
                CustomException.class,
                () -> new AdminTokenService(shortSecret).issue(principal(0))
        );

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED.getCode(), missingException.getCode());
        assertEquals(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED.getCode(), shortException.getCode());
    }

    private AdminSecurityProperties validProperties(byte fill) {
        byte[] secret = new byte[32];
        java.util.Arrays.fill(secret, fill);
        AdminSecurityProperties properties = new AdminSecurityProperties();
        properties.setJwtSecretBase64(Base64.getEncoder().encodeToString(secret));
        properties.setTokenTtl(Duration.ofHours(8));
        properties.setIssuer("point-platform-admin-test");
        return properties;
    }

    private AdminUserPrincipal principal(int tokenVersion) {
        return new AdminUserPrincipal(
                10L,
                "admin",
                "password-hash",
                "管理员",
                SysUserStatus.ACTIVE,
                null,
                tokenVersion,
                Set.of(RoleCode.SUPER_ADMIN),
                Set.of()
        );
    }
}
