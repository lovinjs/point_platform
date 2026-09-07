package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.auth.config.WechatH5Settings;
import com.core.coreboot.platform.customer.auth.model.WechatOAuthAccessToken;
import com.core.coreboot.platform.customer.auth.model.WechatUserProfile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;

@Component
@RequiredArgsConstructor
public class WechatH5ApiClient {
    private static final Logger log = LoggerFactory.getLogger(WechatH5ApiClient.class);
    private static final String API_HOST = "api.weixin.qq.com";

    private final ObjectMapper objectMapper;

    public WechatOAuthAccessToken exchangeCode(WechatH5Settings settings, String code) {
        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(API_HOST)
                .path("/sns/oauth2/access_token")
                .queryParam("appid", settings.appId())
                .queryParam("secret", settings.appSecret())
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .encode()
                .toUri();
        try {
            byte[] responseBody = client(settings).get()
                    .uri(uri)
                    .retrieve()
                    .body(byte[].class);
            WechatTokenResponse response = readJson(
                    "exchange-code",
                    responseBody,
                    WechatTokenResponse.class
            );
            requireSuccessfulResponse("exchange-code", response == null ? null : response.errCode(),
                    response == null ? null : response.errMsg());
            if (response == null
                    || isBlank(response.accessToken())
                    || isBlank(response.openId())
                    || isBlank(response.scope())) {
                throw authFailed();
            }
            return new WechatOAuthAccessToken(
                    response.accessToken(),
                    response.openId(),
                    response.scope(),
                    normalize(response.unionId())
            );
        } catch (RestClientException ex) {
            log.warn("微信网页授权换取 access_token 请求失败: {}", ex.getClass().getSimpleName());
            throw unavailable();
        }
    }

    public WechatUserProfile loadUserProfile(
            WechatH5Settings settings,
            WechatOAuthAccessToken accessToken
    ) {
        if (!containsUserInfoScope(accessToken.scope())) {
            return new WechatUserProfile(accessToken.openId(), null, null, accessToken.unionId());
        }
        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(API_HOST)
                .path("/sns/userinfo")
                .queryParam("access_token", accessToken.accessToken())
                .queryParam("openid", accessToken.openId())
                .queryParam("lang", "zh_CN")
                .build()
                .encode()
                .toUri();
        try {
            byte[] responseBody = client(settings).get()
                    .uri(uri)
                    .retrieve()
                    .body(byte[].class);
            WechatUserInfoResponse response = readJson(
                    "load-user-profile",
                    responseBody,
                    WechatUserInfoResponse.class
            );
            requireSuccessfulResponse("load-user-profile", response == null ? null : response.errCode(),
                    response == null ? null : response.errMsg());
            if (response == null
                    || isBlank(response.openId())
                    || !accessToken.openId().equals(response.openId())) {
                throw authFailed();
            }
            return new WechatUserProfile(
                    response.openId(),
                    normalize(response.nickname()),
                    normalize(response.headImageUrl()),
                    firstNonBlank(response.unionId(), accessToken.unionId())
            );
        } catch (RestClientException ex) {
            log.warn("微信网页授权获取用户资料请求失败: {}", ex.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private RestClient client(WechatH5Settings settings) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(settings.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(settings.readTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    <T> T readJson(String stage, byte[] responseBody, Class<T> responseType) {
        if (responseBody == null || responseBody.length == 0) {
            log.warn("微信网页授权接口返回空响应: stage={}", stage);
            throw unavailable();
        }
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
            log.warn("微信网页授权接口返回无法解析的响应: stage={}, responseBytes={}",
                    stage, responseBody.length);
            throw unavailable();
        }
    }

    private void requireSuccessfulResponse(String stage, Integer errorCode, String errorMessage) {
        if (errorCode == null || errorCode == 0) {
            return;
        }
        log.warn("微信网页授权接口返回错误: stage={}, errcode={}, errmsg={}",
                stage, errorCode, sanitizeLogText(errorMessage));
        throw authFailed();
    }

    private boolean containsUserInfoScope(String scope) {
        if (scope == null) {
            return false;
        }
        for (String part : scope.split("[,\\s]+")) {
            if ("snsapi_userinfo".equals(part)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalized = normalize(preferred);
        return normalized == null ? normalize(fallback) : normalized;
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String sanitizeLogText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200);
    }

    private CustomException authFailed() {
        return new CustomException(ExceptionEnum.PLATFORM_WECHAT_AUTH_FAILED);
    }

    private CustomException unavailable() {
        return new CustomException(ExceptionEnum.PLATFORM_WECHAT_API_UNAVAILABLE);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WechatTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("openid") String openId,
            String scope,
            @JsonProperty("unionid") String unionId,
            @JsonProperty("errcode") Integer errCode,
            @JsonProperty("errmsg") String errMsg
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WechatUserInfoResponse(
            @JsonProperty("openid") String openId,
            String nickname,
            @JsonProperty("headimgurl") String headImageUrl,
            @JsonProperty("unionid") String unionId,
            @JsonProperty("errcode") Integer errCode,
            @JsonProperty("errmsg") String errMsg
    ) {
    }
}
