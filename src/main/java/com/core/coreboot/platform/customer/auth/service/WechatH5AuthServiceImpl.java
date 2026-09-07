package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.auth.config.WechatH5Settings;
import com.core.coreboot.platform.customer.auth.config.WechatH5SettingsProvider;
import com.core.coreboot.platform.customer.auth.model.WechatOAuthAccessToken;
import com.core.coreboot.platform.customer.auth.model.WechatOAuthState;
import com.core.coreboot.platform.customer.auth.model.WechatUserProfile;
import com.core.coreboot.platform.customer.auth.security.CustomerTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class WechatH5AuthServiceImpl implements WechatH5AuthService {
    private static final String AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String H5_CALLBACK_FRAGMENT = "#/pages/auth/callback";

    private final WechatH5SettingsProvider settingsProvider;
    private final WechatOAuthStateStore oauthStateStore;
    private final WechatH5ApiClient wechatApiClient;
    private final WechatCustomerIdentityService customerIdentityService;
    private final CustomerLoginTicketStore loginTicketStore;
    private final CustomerTokenService customerTokenService;

    @Override
    public URI createAuthorizationRedirect(String returnPath) {
        WechatH5Settings settings = requireReady();
        String state = oauthStateStore.issue(returnPath);
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("appid", settings.appId())
                .queryParam("redirect_uri", settings.callbackUrl())
                .queryParam("response_type", "code")
                .queryParam("scope", settings.scope())
                .queryParam("state", state)
                .fragment("wechat_redirect")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    @Override
    public URI completeAuthorization(String code, String state) {
        WechatH5Settings settings = requireReady();
        String normalizedCode = requireCode(code);
        WechatOAuthState oauthState = oauthStateStore.consume(state);
        WechatOAuthAccessToken accessToken = wechatApiClient.exchangeCode(settings, normalizedCode);
        WechatUserProfile profile = wechatApiClient.loadUserProfile(settings, accessToken);
        Long customerId = customerIdentityService.resolveCustomer(settings.appId(), profile);
        String ticket = loginTicketStore.issue(customerId, oauthState.returnPath());
        return URI.create(settings.h5BaseUrl()
                + "/?loginTicket=" + ticket
                + H5_CALLBACK_FRAGMENT);
    }

    private WechatH5Settings requireReady() {
        WechatH5Settings settings = settingsProvider.requireValid();
        customerTokenService.assertConfigured();
        return settings;
    }

    private String requireCode(String code) {
        if (code == null || code.isBlank() || code.length() > 512) {
            throw new CustomException(ExceptionEnum.PLATFORM_WECHAT_AUTH_FAILED);
        }
        return code.trim();
    }
}
