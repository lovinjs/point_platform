package com.core.coreboot.platform.auth.security;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.exception.ExceptionEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AdminSecurityResponseWriter {
    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int status, ExceptionEnum error) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiRestResponse.error(error));
    }
}
