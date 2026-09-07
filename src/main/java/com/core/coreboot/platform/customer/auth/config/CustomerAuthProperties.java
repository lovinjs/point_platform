package com.core.coreboot.platform.customer.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.security.customer")
public class CustomerAuthProperties {
    private String jwtSecretBase64;
    private Duration tokenTtl = Duration.ofHours(2);
    private Duration loginTicketTtl = Duration.ofMinutes(1);
    private String issuer = "point-platform-customer";

    public String getJwtSecretBase64() {
        return jwtSecretBase64;
    }

    public void setJwtSecretBase64(String jwtSecretBase64) {
        this.jwtSecretBase64 = jwtSecretBase64;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public Duration getLoginTicketTtl() {
        return loginTicketTtl;
    }

    public void setLoginTicketTtl(Duration loginTicketTtl) {
        this.loginTicketTtl = loginTicketTtl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
