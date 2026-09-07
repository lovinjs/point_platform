package com.core.coreboot.platform.customer.auth.security;

import com.core.coreboot.exception.ExceptionEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomerAccessDeniedHandler implements AccessDeniedHandler {
    private final CustomerSecurityResponseWriter responseWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                ExceptionEnum.PLATFORM_CUSTOMER_ACCESS_DENIED);
    }
}
