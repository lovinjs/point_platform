package com.core.coreboot.platform.consumption.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "platform.consumption")
public class ConsumptionProperties {
    private Duration pendingTtl = Duration.ofMinutes(5);

    public Duration getPendingTtl() {
        return pendingTtl;
    }

    public void setPendingTtl(Duration pendingTtl) {
        this.pendingTtl = pendingTtl;
    }
}
