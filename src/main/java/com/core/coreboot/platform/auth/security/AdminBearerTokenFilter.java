package com.core.coreboot.platform.auth.security;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class AdminBearerTokenFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminTokenService adminTokenService;
    private final AdminUserDetailsService userDetailsService;
    private final AdminSecurityResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            AdminTokenService.VerifiedAdminToken verifiedToken = adminTokenService.verify(token);
            AdminUserPrincipal principal = userDetailsService.loadByUserId(verifiedToken.userId());
            if (!principal.isEnabled()
                    || !principal.isAccountNonLocked()
                    || principal.getTokenVersion() != verifiedToken.tokenVersion()) {
                throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
            }

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } catch (CustomException | UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
        }
    }
}
