package com.core.coreboot.filter;

import com.core.coreboot.user.entity.User;
import com.core.coreboot.utils.TokenUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 用户过滤器
 */
public class UserRoleFilter implements Filter {

    public static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public static User getCurrentUser() {
        return currentUser.get();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 设置响应编码，防止中文乱码
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        response.setContentType("application/json;charset=utf-8");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        try {
            String token = TokenUtils.getToken(request);
            if (token == null) {
                // 未登录 - 返回401状态码
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                PrintWriter writer = new HttpServletResponseWrapper((HttpServletResponse) servletResponse).getWriter();
                writer.write("{\n" +
                    "\"code\": 10007,\n" +
                    "\"msg\": \"用户未登录\",\n" +
                    "\"data\": null\n" +
                    "}");
                writer.flush();
                writer.close();
                return;
            }
            User user = TokenUtils.verifyToken(token);
            currentUser.set(user);
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            currentUser.remove();
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
