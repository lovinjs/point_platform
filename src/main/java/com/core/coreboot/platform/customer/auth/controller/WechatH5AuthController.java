package com.core.coreboot.platform.customer.auth.controller;

import com.core.coreboot.platform.customer.auth.service.WechatH5AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "积分平台 - 微信H5登录")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/h5/auth/wechat")
public class WechatH5AuthController {
    private final WechatH5AuthService wechatH5AuthService;

    @Operation(summary = "发起微信网页授权", description = "通过浏览器 302 跳转到微信授权页面")
    @GetMapping("/start")
    public ResponseEntity<Void> start(
            @Parameter(description = "授权完成后的站内页面，目前仅支持首页")
            @RequestParam(name = "returnPath", required = false) String returnPath
    ) {
        return redirect(wechatH5AuthService.createAuthorizationRedirect(returnPath));
    }

    @Operation(summary = "接收微信网页授权回调", description = "完成身份匹配后携带一次性票据跳转回 H5")
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state
    ) {
        return redirect(wechatH5AuthService.completeAuthorization(code, state));
    }

    private ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(302)
                .location(location)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("Referrer-Policy", "no-referrer")
                .build();
    }
}
