package com.core.coreboot.platform.customer.auth.config;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WechatH5SettingsProvider {
    private static final Set<String> SUPPORTED_SCOPES = Set.of("snsapi_base", "snsapi_userinfo");
    private static final Duration MAXIMUM_STATE_TTL = Duration.ofMinutes(10);
    private static final Duration MAXIMUM_HTTP_TIMEOUT = Duration.ofSeconds(30);
    private static final String CALLBACK_PATH = "/api/v1/h5/auth/wechat/callback";

    private final WechatH5AuthProperties properties;

    public WechatH5Settings requireValid() {
        if (!properties.isEnabled()) {
            throw notConfigured();
        }
        String appId = requireText(properties.getAppId());
        String appSecret = requireText(properties.getAppSecret());
        String callbackUrl = requireHttpUrl(properties.getCallbackUrl(), false, CALLBACK_PATH);
        String h5BaseUrl = requireHttpUrl(properties.getH5BaseUrl(), true, null);
        String scope = requireText(properties.getScope());
        if (!SUPPORTED_SCOPES.contains(scope)) {
            throw notConfigured();
        }
        Duration stateTtl = requireDuration(properties.getStateTtl(), MAXIMUM_STATE_TTL);
        Duration connectTimeout = requireDuration(properties.getConnectTimeout(), MAXIMUM_HTTP_TIMEOUT);
        Duration readTimeout = requireDuration(properties.getReadTimeout(), MAXIMUM_HTTP_TIMEOUT);
        return new WechatH5Settings(
                appId,
                appSecret,
                callbackUrl,
                h5BaseUrl,
                scope,
                stateTtl,
                connectTimeout,
                readTimeout
        );
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw notConfigured();
        }
        return value.trim();
    }

    private String requireHttpUrl(String value, boolean baseUrl, String requiredPath) {
        String normalized = requireText(value);
        try {
            URI uri = URI.create(normalized);
            boolean http = "http".equalsIgnoreCase(uri.getScheme());
            boolean https = "https".equalsIgnoreCase(uri.getScheme());
            if ((!http && !https)
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw notConfigured();
            }
            String path = uri.getPath();
            if (baseUrl && path != null && !path.isBlank() && !"/".equals(path)) {
                throw notConfigured();
            }
            if (requiredPath != null && !requiredPath.equals(path)) {
                throw notConfigured();
            }
            return baseUrl && normalized.endsWith("/")
                    ? normalized.substring(0, normalized.length() - 1)
                    : normalized;
        } catch (IllegalArgumentException ex) {
            throw notConfigured();
        }
    }

    private Duration requireDuration(Duration value, Duration maximum) {
        if (value == null || value.isZero() || value.isNegative() || value.compareTo(maximum) > 0) {
            throw notConfigured();
        }
        return value;
    }

    private CustomException notConfigured() {
        return new CustomException(ExceptionEnum.PLATFORM_WECHAT_AUTH_NOT_CONFIGURED);
    }
}
