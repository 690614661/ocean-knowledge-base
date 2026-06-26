package com.ocean.websocket;

import com.ocean.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/api/ws")
public class WebSocketServer {

    /** userId → Session 映射 */
    private static final Map<Long, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        // 从查询参数中获取 token
        String queryString = session.getQueryString();
        String token = parseTokenFromQuery(queryString);

        if (token == null || !JwtUtil.validateToken(token)) {
            log.warn("WebSocket 连接拒绝：无效 token");
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "无效的 token"));
            } catch (IOException e) {
                log.error("关闭 WebSocket 异常", e);
            }
            return;
        }

        Long userId = JwtUtil.getUserIdFromToken(token);
        // 关闭该用户的旧连接
        Session oldSession = SESSION_MAP.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (IOException e) {
                log.warn("关闭旧 WebSocket 连接异常", e);
            }
        }
        SESSION_MAP.put(userId, session);
        log.info("WebSocket 连接建立：userId={}, sessionId={}", userId, session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = findUserIdBySession(session);
        if (userId != null) {
            SESSION_MAP.remove(userId);
            log.info("WebSocket 连接关闭：userId={}", userId);
        } else {
            SESSION_MAP.values().remove(session);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("WebSocket 收到消息：{}", message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 错误：sessionId={}", session.getId(), error);
        Long userId = findUserIdBySession(session);
        if (userId != null) {
            SESSION_MAP.remove(userId);
        } else {
            SESSION_MAP.values().remove(session);
        }
    }

    /**
     * 向指定用户推送消息
     */
    public static void sendMessageToUser(Long userId, String message) {
        Session session = SESSION_MAP.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                log.info("WebSocket 推送成功：userId={}", userId);
            } catch (IOException e) {
                log.error("WebSocket 推送失败：userId={}", userId, e);
                SESSION_MAP.remove(userId);
            }
        } else {
            log.warn("WebSocket 用户不在线：userId={}", userId);
        }
    }

    public static int getOnlineCount() {
        return SESSION_MAP.size();
    }

    /**
     * 向所有在线用户广播消息
     */
    public static void broadcastMessage(String message) {
        SESSION_MAP.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.warn("WebSocket 广播失败：userId={}", userId, e);
                }
            }
        });
    }

    private static String parseTokenFromQuery(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && "token".equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }

    private Long findUserIdBySession(Session session) {
        for (Map.Entry<Long, Session> entry : SESSION_MAP.entrySet()) {
            if (entry.getValue().equals(session)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
