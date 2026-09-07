package com.core.coreboot.platform.customer.auth.security;

import com.core.coreboot.exception.ExceptionEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomerAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final CustomerSecurityResponseWriter responseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                ExceptionEnum.PLATFORM_CUSTOMER_TOKEN_INVALID);
    }
}
