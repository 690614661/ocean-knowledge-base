package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.common.Constant;
import com.ocean.common.PageResp;
import com.ocean.domain.Note;
import com.ocean.domain.dto.BatchDeleteReq;
import com.ocean.domain.dto.NoteSaveReq;
import com.ocean.interceptor.RateLimit;
import com.ocean.service.NoteService;
import com.ocean.service.NotificationService;
import com.ocean.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import com.ocean.util.RequestUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Api(tags = "用户笔记")
@RestController
@RequestMapping("/api/note")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationService notificationService;

    @ApiOperation("我的笔记列表")
    @GetMapping("/list")
    public CommonResp<PageResp<Note>> myList(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || !JwtUtil.validateToken(token)) {
            return CommonResp.fail("登录已过期，请重新登录");
        }
        Long userId = JwtUtil.getUserIdFromToken(token);
        return CommonResp.ok(noteService.myList(page, size, userId));
    }

    @ApiOperation("公开笔记列表")
    @GetMapping("/public")
    public CommonResp<PageResp<Note>> publicList(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String keyword) {
        return CommonResp.ok(noteService.publicList(page, size, keyword));
    }

    @ApiOperation("笔记详情")
    @GetMapping("/{id}")
    public CommonResp<Note> detail(@PathVariable Long id) {
        return CommonResp.ok(noteService.getDetail(id));
    }

    @ApiOperation("新增/编辑笔记")
    @PostMapping("/save")
    public CommonResp<?> save(@Validated @RequestBody NoteSaveReq req, HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || !JwtUtil.validateToken(token)) {
            return CommonResp.fail("登录已过期，请重新登录");
        }
        Long userId = JwtUtil.getUserIdFromToken(token);
        noteService.save(req, userId);
        return CommonResp.ok("保存成功");
    }

    @ApiOperation("删除笔记")
    @DeleteMapping("/delete/{id}")
    public CommonResp<?> delete(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || !JwtUtil.validateToken(token)) {
            return CommonResp.fail("登录已过期，请重新登录");
        }
        Long userId = JwtUtil.getUserIdFromToken(token);
        noteService.delete(id, userId);
        return CommonResp.ok("删除成功");
    }

    @ApiOperation("批量删除笔记")
    @PostMapping("/delete/batch")
    public CommonResp<?> deleteBatch(@Validated @RequestBody BatchDeleteReq req, HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || !JwtUtil.validateToken(token)) {
            return CommonResp.fail("登录已过期，请重新登录");
        }
        Long userId = JwtUtil.getUserIdFromToken(token);
        noteService.deleteBatch(req.getIds(), userId);
        return CommonResp.ok("批量删除成功");
    }

    @ApiOperation("笔记点赞")
    @RateLimit(permitsPerMinute = 30)
    @PostMapping("/vote/{id}")
    public CommonResp<?> vote(@PathVariable Long id, HttpServletRequest request) {
        String ip = RequestUtil.getClientIp();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "note_vote:" + id + ":" + ip + ":" + today;

        Boolean setSuccess = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 24, TimeUnit.HOURS);
        if (setSuccess == null || !setSuccess) {
            return CommonResp.fail("今日已点赞");
        }

        noteService.vote(id, ip);

        // 发送通知给笔记作者
        try {
            Note note = noteService.getById(id);
            if (note != null) {
                String token = request.getHeader("token");
                if (token != null && JwtUtil.validateToken(token)) {
                    Long voterId = JwtUtil.getUserIdFromToken(token);
                    String voterName = JwtUtil.getNameFromToken(token);
                    if (!note.getUserId().equals(voterId)) {
                        notificationService.send(
                                voterId, note.getUserId(), "vote",
                                "用户 " + voterName + " 赞了你的笔记《" + note.getTitle() + "》",
                                null, note.getId()
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("发送点赞通知失败", e);
        }

        return CommonResp.ok("点赞成功");
    }
}
