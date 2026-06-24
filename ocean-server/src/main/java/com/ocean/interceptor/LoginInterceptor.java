package com.ocean.interceptor;

import com.ocean.common.Constant;
import com.ocean.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(Constant.TOKEN_HEADER);
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"登录已过期，请重新登录\",\"content\":null}");
            return false;
        }

        // JWT 校验
        if (!JwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"登录已过期，请重新登录\",\"content\":null}");
            return false;
        }

        // Redis 校验
        Long userId = JwtUtil.getUserIdFromToken(token);
        String redisKey = Constant.TOKEN_REDIS_PREFIX + userId;
        Object redisToken = redisTemplate.opsForValue().get(redisKey);
        if (redisToken == null || !token.equals(redisToken)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"登录已过期，请重新登录\",\"content\":null}");
            return false;
        }

        // 刷新 Token 过期时间
        redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);

        // 刷新在线状态（用户每发一次请求，记录活跃时间）
        try {
            String onlineKey = Constant.ONLINE_USER_PREFIX + userId;
            Object onlineObj = redisTemplate.opsForValue().get(onlineKey);
            @SuppressWarnings("unchecked")
            Map<String, Object> onlineInfo;
            if (onlineObj instanceof Map<?, ?>) {
                onlineInfo = (Map<String, Object>) onlineObj;
            } else {
                // 首次：从 Token 解析用户信息创建在线记录
                onlineInfo = new java.util.HashMap<>();
                onlineInfo.put("userId", userId);
                onlineInfo.put("loginName", JwtUtil.getLoginNameFromToken(token));
                onlineInfo.put("name", JwtUtil.getNameFromToken(token));
                onlineInfo.put("role", JwtUtil.getRoleFromToken(token));
            }
            onlineInfo.put("lastAccess", System.currentTimeMillis());
            redisTemplate.opsForValue().set(onlineKey, onlineInfo,
                    Constant.ONLINE_USER_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("刷新在线状态失败: userId={}", userId, e);
        }

        return true;
    }
}
