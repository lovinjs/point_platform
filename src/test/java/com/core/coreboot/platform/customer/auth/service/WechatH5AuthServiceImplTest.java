package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.platform.customer.auth.config.WechatH5Settings;
import com.core.coreboot.platform.customer.auth.config.WechatH5SettingsProvider;
import com.core.coreboot.platform.customer.auth.model.WechatOAuthAccessToken;
import com.core.coreboot.platform.customer.auth.model.WechatOAuthState;
import com.core.coreboot.platform.customer.auth.model.WechatUserProfile;
import com.core.coreboot.platform.customer.auth.security.CustomerTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatH5AuthServiceImplTest {
    private static final String STATE = "abcdefghijklmnopqrstuvwxyzABCDEFGH123456789";
    private static final String TICKET = "987654321HGFEDCBAzyxwvutsrqponmlkjihgfedcba";

    @Mock
    private WechatH5SettingsProvider settingsProvider;
    @Mock
    private WechatOAuthStateStore oauthStateStore;
    @Mock
    private WechatH5ApiClient wechatApiClient;
    @Mock
    private WechatCustomerIdentityService customerIdentityService;
    @Mock
    private CustomerLoginTicketStore loginTicketStore;
    @Mock
    private CustomerTokenService customerTokenService;

    private WechatH5Settings settings;
    private WechatH5AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        settings = new WechatH5Settings(
                "wx-test-app",
                "test-secret",
                "http://192.168.1.220:5173/api/v1/h5/auth/wechat/callback",
                "http://192.168.1.220:5173",
                "snsapi_userinfo",
                Duration.ofMinutes(5),
                Duration.ofSeconds(5),
                Duration.ofSeconds(8)
        );
        service = new WechatH5AuthServiceImpl(
                settingsProvider,
                oauthStateStore,
                wechatApiClient,
                customerIdentityService,
                loginTicketStore,
                customerTokenService
        );
        when(settingsProvider.requireValid()).thenReturn(settings);
    }

    @Test
    void shouldCreateWechatAuthorizationRedirect() {
        when(oauthStateStore.issue("/pages/index/index")).thenReturn(STATE);

        URI redirect = service.createAuthorizationRedirect("/pages/index/index");

        assertEquals("https", redirect.getScheme());
        assertEquals("open.weixin.qq.com", redirect.getHost());
        assertEquals("/connect/oauth2/authorize", redirect.getPath());
        assertEquals("wechat_redirect", redirect.getFragment());
        Map<String, String> query = decodedQuery(redirect);
        assertEquals("wx-test-app", query.get("appid"));
        assertEquals(settings.callbackUrl(), query.get("redirect_uri"));
        assertEquals("code", query.get("response_type"));
        assertEquals("snsapi_userinfo", query.get("scope"));
        assertEquals(STATE, query.get("state"));
        verify(customerTokenService).assertConfigured();
    }

    @Test
    void shouldCompleteAuthorizationAndRedirectToH5WithOneTimeTicket() {
        WechatOAuthState oauthState = new WechatOAuthState("/pages/index/index");
        WechatOAuthAccessToken accessToken = new WechatOAuthAccessToken(
                "wechat-access-token", "open-id", "snsapi_userinfo", "union-id"
        );
        WechatUserProfile profile = new WechatUserProfile(
                "open-id", "微信用户", "https://example.test/avatar.jpg", "union-id"
        );
        when(oauthStateStore.consume(STATE)).thenReturn(oauthState);
        when(wechatApiClient.exchangeCode(settings, "authorization-code")).thenReturn(accessToken);
        when(wechatApiClient.loadUserProfile(settings, accessToken)).thenReturn(profile);
        when(customerIdentityService.resolveCustomer("wx-test-app", profile)).thenReturn(12L);
        when(loginTicketStore.issue(12L, "/pages/index/index")).thenReturn(TICKET);

        URI redirect = service.completeAuthorization(" authorization-code ", STATE);

        assertEquals(
                "http://192.168.1.220:5173/?loginTicket=" + TICKET
                        + "#/pages/auth/callback",
                redirect.toString()
        );
        InOrder order = inOrder(
                settingsProvider,
                customerTokenService,
                oauthStateStore,
                wechatApiClient,
                customerIdentityService,
                loginTicketStore
        );
        order.verify(settingsProvider).requireValid();
        order.verify(customerTokenService).assertConfigured();
        order.verify(oauthStateStore).consume(STATE);
        order.verify(wechatApiClient).exchangeCode(settings, "authorization-code");
        order.verify(wechatApiClient).loadUserProfile(settings, accessToken);
        order.verify(customerIdentityService).resolveCustomer("wx-test-app", profile);
        order.verify(loginTicketStore).issue(12L, "/pages/index/index");
    }

    private Map<String, String> decodedQuery(URI uri) {
        assertTrue(uri.getRawQuery() != null && !uri.getRawQuery().isBlank());
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> decode(pair[0]),
                        pair -> pair.length == 1 ? "" : decode(pair[1])
                ));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
