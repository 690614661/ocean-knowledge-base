package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.common.Constant;
import com.ocean.domain.Doc;
import com.ocean.domain.dto.BatchDeleteReq;
import com.ocean.domain.dto.DocSaveReq;
import com.ocean.interceptor.RateLimit;
import com.ocean.service.DocService;
import com.ocean.util.JwtUtil;
import com.ocean.util.RequestUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Api(tags = "文档管理")
@Slf4j
@RestController
@RequestMapping("/api/doc")
public class DocController {

    @Autowired
    private DocService docService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @ApiOperation("文档树形列表")
    @GetMapping("/list")
    public CommonResp<List<Doc>> tree(@RequestParam Long ebookId) {
        return CommonResp.ok(docService.tree(ebookId));
    }

    @ApiOperation("文档详情（阅读）")
    @GetMapping("/{id}")
    public CommonResp<Doc> detail(@PathVariable Long id, HttpServletRequest request) {
        // 尝试获取登录用户（阅读历史用，非必须）
        Long userId = null;
        String token = request.getHeader("token");
        if (token != null && JwtUtil.validateToken(token)) {
            userId = JwtUtil.getUserIdFromToken(token);
        }
        return CommonResp.ok(docService.getDetail(id, userId));
    }

    @ApiOperation("新增/编辑文档")
    @PostMapping("/save")
    public CommonResp<?> save(@Validated @RequestBody DocSaveReq req) {
        docService.save(req);
        return CommonResp.ok("保存成功");
    }

    @ApiOperation("删除文档")
    @DeleteMapping("/delete/{id}")
    public CommonResp<?> delete(@PathVariable Long id) {
        docService.delete(id);
        return CommonResp.ok("删除成功");
    }

    @ApiOperation("批量删除文档")
    @PostMapping("/delete/batch")
    public CommonResp<?> deleteBatch(@Validated @RequestBody BatchDeleteReq req) {
        docService.deleteBatch(req.getIds());
        return CommonResp.ok("批量删除成功");
    }

    @ApiOperation("文档点赞")
    @RateLimit(permitsPerMinute = 30)
    @PostMapping("/vote/{id}")
    public CommonResp<?> vote(@PathVariable Long id, HttpServletRequest request) {
        String ip = RequestUtil.getClientIp();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = Constant.VOTE_REDIS_PREFIX + id + ":" + ip + ":" + today;

        // Redis 防重
        Boolean setSuccess = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 24, TimeUnit.HOURS);
        if (setSuccess == null || !setSuccess) {
            return CommonResp.fail("今日已点赞");
        }

        docService.vote(id, ip);

        // 发送MQ消息，触发WebSocket通知
        if (rocketMQTemplate != null) {
            try {
                rocketMQTemplate.convertAndSend("VOTE_TOPIC", "文档 " + id + " 被点赞");
            } catch (Exception e) {
                log.warn("MQ消息发送失败", e);
            }
        }

        return CommonResp.ok("点赞成功");
    }
}
