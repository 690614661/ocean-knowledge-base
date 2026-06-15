package com.ocean.interceptor;

import com.ocean.common.Constant;
import com.ocean.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理员权限拦截器 — 校验当前用户是否为管理员
 */
@Slf4j
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(Constant.TOKEN_HEADER);
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录\",\"content\":null}");
            return false;
        }

        // 从 JWT 中提取角色
        String role = JwtUtil.getRoleFromToken(token);
        if (!Constant.ROLE_ADMIN.equals(role)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"无权限访问，仅管理员可操作\",\"content\":null}");
            return false;
        }

        return true;
    }
}
