package com.ocean.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.PageResp;
import com.ocean.domain.Notification;
import com.ocean.mapper.NotificationMapper;
import com.ocean.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 发送通知：写入数据库 + WebSocket 推送
     */
    public void send(Long fromUserId, Long toUserId, String type, String title,
                     String content, Long relatedId) {
        if (toUserId == null) return;

        Notification notification = new Notification();
        notification.setFromUserId(fromUserId);
        notification.setToUserId(toUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        this.save(notification);

        // WebSocket 推送
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("type", "notification");

        Map<String, Object> data = new HashMap<>();
        data.put("id", notification.getId());
        data.put("type", type);
        data.put("title", title);
        data.put("relatedId", relatedId);
        data.put("createTime", notification.getCreateTime());
        wsMessage.put("data", data);

        WebSocketServer.sendMessageToUser(toUserId, JSON.toJSONString(wsMessage));

        // 推送未读计数
        sendUnreadCount(toUserId);

        log.debug("通知已发送: toUserId={}, type={}, title={}", toUserId, type, title);
    }

    /**
     * 推送未读计数
     */
    public void sendUnreadCount(Long userId) {
        int count = notificationMapper.countUnread(userId);
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("type", "unread_count");
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        wsMessage.put("data", data);
        WebSocketServer.sendMessageToUser(userId, JSON.toJSONString(wsMessage));
    }

    /**
     * 获取通知列表
     */
    public PageResp<Notification> listByUser(Long userId, int page, int size) {
        IPage<Notification> notifPage = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getToUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
        return new PageResp<>(notifPage.getTotal(), notifPage.getRecords());
    }

    /**
     * 未读数
     */
    public int unreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    /**
     * 标记已读
     */
    public void markRead(Long id, Long userId) {
        notificationMapper.markRead(id, userId);
        sendUnreadCount(userId);
    }

    /**
     * 全部已读
     */
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
        sendUnreadCount(userId);
    }
}
