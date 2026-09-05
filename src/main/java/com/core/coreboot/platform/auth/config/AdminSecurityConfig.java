package com.core.coreboot.platform.auth.config;

import com.core.coreboot.platform.auth.security.AdminAccessDeniedHandler;
import com.core.coreboot.platform.auth.security.AdminAuthenticationEntryPoint;
import com.core.coreboot.platform.auth.security.AdminBearerTokenFilter;
import com.core.coreboot.platform.auth.security.AdminSecurityResponseWriter;
import com.core.coreboot.platform.auth.security.AdminTokenService;
import com.core.coreboot.platform.auth.security.AdminUserDetailsService;
import com.core.coreboot.platform.bootstrap.config.AdminBootstrapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({AdminSecurityProperties.class, AdminBootstrapProperties.class})
public class AdminSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider adminAuthenticationProvider(
            AdminUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager adminAuthenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            AdminTokenService adminTokenService,
            AdminUserDetailsService userDetailsService,
            AdminSecurityResponseWriter responseWriter,
            AdminAuthenticationEntryPoint authenticationEntryPoint,
            AdminAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        AdminBearerTokenFilter bearerTokenFilter =
                new AdminBearerTokenFilter(
                        adminTokenService,
                        userDetailsService,
                        responseWriter
                );
        http
                .securityMatcher("/api/v1/admin/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/admin/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
