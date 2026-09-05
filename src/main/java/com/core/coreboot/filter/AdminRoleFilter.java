package com.core.coreboot.filter;

import com.core.coreboot.user.entity.User;
import com.core.coreboot.user.service.UserService;
import com.core.coreboot.utils.TokenUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 管理员角色过滤器
 */
public class AdminRoleFilter implements Filter {

    private final UserService userService;

    public AdminRoleFilter(UserService userService) {
        this.userService = userService;
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
        User currentUser = TokenUtils.verifyToken(token);
        boolean isAdmin = userService.checkIsAdmin(currentUser);
        if (isAdmin) {
            // 已登录且是管理员，放行
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // 非管理员 - 返回403状态码
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            PrintWriter writer = new HttpServletResponseWrapper((HttpServletResponse) servletResponse).getWriter();
            writer.write("{\n" +
                "\"code\": 10009,\n" +
                "\"msg\": \"无管理员权限\",\n" +
                "\"data\": null\n" +
                "}");
            writer.flush();
            writer.close();
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
