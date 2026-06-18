package com.ocean.controller;

import com.ocean.ai.service.AiService;
import com.ocean.common.CommonResp;
import com.ocean.domain.AiConversation;
import com.ocean.domain.AiMessage;
import com.ocean.interceptor.RateLimit;
import com.ocean.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "AI 功能")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @ApiOperation("AI 多轮对话")
    @RateLimit(permitsPerMinute = 10, permitsPerDay = 100)
    @PostMapping("/chat")
    public CommonResp<Map<String, Object>> chat(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        String conversationId = body.get("conversationId");
        String message = body.get("message");
        return CommonResp.ok(aiService.chat(conversationId, message, userId));
    }

    @ApiOperation("AI 内容生成")
    @RateLimit(permitsPerMinute = 5)
    @PostMapping("/generate")
    public CommonResp<Map<String, Object>> generate(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        String type = body.getOrDefault("type", "note_generate");
        String topic = body.get("topic");
        String selectedText = body.get("selectedText");
        return CommonResp.ok("生成成功", aiService.generate(type, topic, selectedText, userId));
    }

    @ApiOperation("对话会话列表")
    @GetMapping("/conversations")
    public CommonResp<List<AiConversation>> conversations(HttpServletRequest request,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        return CommonResp.ok(aiService.getConversations(userId, page, size));
    }

    @ApiOperation("会话消息历史")
    @GetMapping("/conversations/{id}/messages")
    public CommonResp<List<AiMessage>> messages(@PathVariable String id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        return CommonResp.ok(aiService.getMessages(id, userId));
    }

    @ApiOperation("删除对话")
    @DeleteMapping("/conversations/{id}")
    public CommonResp<?> deleteConversation(@PathVariable String id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        aiService.deleteConversation(id, userId);
        return CommonResp.ok("删除成功");
    }

    @ApiOperation("AI 用量统计")
    @GetMapping("/usage")
    public CommonResp<Map<String, Object>> usage(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        return CommonResp.ok(aiService.getUsage(userId));
    }
}
