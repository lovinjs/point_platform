package com.core.coreboot.platform.deployment;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Base64;

@Component
@Profile("prod")
public class ProductionConfigurationValidator implements InitializingBean {
    private final Environment environment;

    public ProductionConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String url = required("spring.datasource.url");
        if (!url.startsWith("jdbc:mysql://") || url.contains("jdbc:p6spy:")) {
            throw invalid("DB_URL", "必须使用 jdbc:mysql:// 连接地址，服务器模式不启用 SQL 明文日志");
        }
        if ("root".equalsIgnoreCase(required("spring.datasource.username").trim())) {
            throw invalid("DB_USERNAME", "必须使用专用业务账号，不能使用 root");
        }
        required("spring.datasource.password");
        required("spring.data.redis.host");
        required("spring.data.redis.password");

        byte[] adminSecret = jwtSecret("platform.security.admin.jwt-secret-base64");
        byte[] customerSecret = jwtSecret("platform.security.customer.jwt-secret-base64");
        if (MessageDigest.isEqual(adminSecret, customerSecret)) {
            throw invalid("JWT", "后台与消费者必须使用两个不同的随机密钥");
        }
        if ("LOG".equalsIgnoreCase(environment.getProperty("platform.phone-verification.mode", "DISABLED"))) {
            throw invalid("platform.phone-verification.mode", "服务器模式不能把短信验证码输出到日志");
        }
        if (!environment.getProperty("spring.flyway.enabled", Boolean.class, true)
                || !environment.getProperty("spring.flyway.clean-disabled", Boolean.class, true)
                || !environment.getProperty("spring.flyway.validate-on-migrate", Boolean.class, true)
                || environment.getProperty("spring.flyway.baseline-on-migrate", Boolean.class, false)) {
            throw invalid("Flyway", "必须启用迁移及校验、禁用 clean 和自动 baseline");
        }
    }

    private String required(String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank() || value.contains("${")) {
            throw invalid(property, "未填写必要配置");
        }
        return value;
    }

    private byte[] jwtSecret(String property) {
        try {
            byte[] secret = Base64.getDecoder().decode(required(property).trim());
            if (secret.length < 32) {
                throw invalid(property, "Base64 解码后至少需要 32 字节");
            }
            return secret;
        } catch (IllegalArgumentException ex) {
            throw invalid(property, "必须是有效的 Base64 随机密钥");
        }
    }

    private IllegalStateException invalid(String property, String reason) {
        // Never include configured values, connection URLs or credentials in errors.
        return new IllegalStateException("服务器配置无效：" + property + "；" + reason);
    }
}
