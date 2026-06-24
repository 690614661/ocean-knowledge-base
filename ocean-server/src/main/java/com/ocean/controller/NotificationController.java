package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.common.PageResp;
import com.ocean.domain.Notification;
import com.ocean.service.NotificationService;
import com.ocean.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "消息通知")
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @ApiOperation("通知列表")
    @GetMapping("/list")
    public CommonResp<PageResp<Notification>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   HttpServletRequest request) {
        Long userId = JwtUtil.getUserIdFromToken(request.getHeader("token"));
        return CommonResp.ok(notificationService.listByUser(userId, page, size));
    }

    @ApiOperation("未读通知数")
    @GetMapping("/unread/count")
    public CommonResp<Map<String, Object>> unreadCount(HttpServletRequest request) {
        Long userId = JwtUtil.getUserIdFromToken(request.getHeader("token"));
        Map<String, Object> result = new HashMap<>();
        result.put("count", notificationService.unreadCount(userId));
        return CommonResp.ok(result);
    }

    @ApiOperation("标记已读")
    @PostMapping("/read/{id}")
    public CommonResp<?> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = JwtUtil.getUserIdFromToken(request.getHeader("token"));
        notificationService.markRead(id, userId);
        return CommonResp.ok("操作成功");
    }

    @ApiOperation("全部已读")
    @PostMapping("/read/all")
    public CommonResp<?> markAllRead(HttpServletRequest request) {
        Long userId = JwtUtil.getUserIdFromToken(request.getHeader("token"));
        notificationService.markAllRead(userId);
        return CommonResp.ok("操作成功");
    }
}
