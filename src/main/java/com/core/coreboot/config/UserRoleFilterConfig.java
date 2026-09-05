package com.core.coreboot.config;

import com.core.coreboot.filter.UserRoleFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserRoleFilterConfig {

    @Bean
    public UserRoleFilter userRoleFilter() {
        return new UserRoleFilter();
    }

    @Bean(name = "userRoleFilterConfigBean")
    public FilterRegistrationBean userRoleFilterConfig() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(userRoleFilter());
        filterRegistrationBean.addUrlPatterns("/cart/*");
        filterRegistrationBean.addUrlPatterns("/order/*");
        filterRegistrationBean.addUrlPatterns("/user/update");
        filterRegistrationBean.addUrlPatterns("/user/info");
        filterRegistrationBean.setName("userRoleFilterConfig");
        return filterRegistrationBean;
    }
}
