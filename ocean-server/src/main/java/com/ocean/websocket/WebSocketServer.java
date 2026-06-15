package com.ocean.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws")
public class WebSocketServer {

    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        log.info("WebSocket连接建立: {}", session.getId());
        SESSION_MAP.put(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        log.info("WebSocket连接关闭: {}", session.getId());
        SESSION_MAP.remove(session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("WebSocket收到消息: {}", message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误: {}", session.getId(), error);
        SESSION_MAP.remove(session.getId());
    }

    public static void sendMessage(String message) {
        SESSION_MAP.forEach((id, session) -> {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                log.error("WebSocket发送消息失败: {}", id, e);
            }
        });
    }

    public static int getOnlineCount() {
        return SESSION_MAP.size();
    }
}
