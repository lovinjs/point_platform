package com.core.coreboot.platform.customer.auth.config;

import com.core.coreboot.platform.customer.auth.security.CustomerAccessDeniedHandler;
import com.core.coreboot.platform.customer.auth.security.CustomerAuthenticationEntryPoint;
import com.core.coreboot.platform.customer.auth.security.CustomerBearerTokenFilter;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipalService;
import com.core.coreboot.platform.customer.auth.security.CustomerSecurityResponseWriter;
import com.core.coreboot.platform.customer.auth.security.CustomerTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties({CustomerAuthProperties.class, WechatH5AuthProperties.class})
public class CustomerSecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain customerSecurityFilterChain(
            HttpSecurity http,
            CustomerTokenService tokenService,
            CustomerPrincipalService principalService,
            CustomerSecurityResponseWriter responseWriter,
            CustomerAuthenticationEntryPoint authenticationEntryPoint,
            CustomerAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        CustomerBearerTokenFilter bearerTokenFilter =
                new CustomerBearerTokenFilter(tokenService, principalService, responseWriter);

        http
                .securityMatcher("/api/v1/customer/**", "/api/v1/h5/auth/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/h5/auth/**").permitAll()
                        .anyRequest().hasRole("CUSTOMER")
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
