package com.core.coreboot.config;

import com.core.coreboot.filter.AdminRoleFilter;
import com.core.coreboot.user.service.UserService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AdminRoleFilterConfig {

    private final UserService userService;

    @Bean
    public AdminRoleFilter adminRoleFilter() {
        return new AdminRoleFilter(userService);
    }

    @Bean(name = "adminRoleFilterConfigBean")
    public FilterRegistrationBean adminRoleFilterConfig() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(adminRoleFilter());
        filterRegistrationBean.addUrlPatterns("/admin/category/*");
        filterRegistrationBean.addUrlPatterns("/admin/product/*");
        filterRegistrationBean.addUrlPatterns("/admin/order/*");
        filterRegistrationBean.setName("adminRoleFilterConfig");
        return filterRegistrationBean;
    }
}
