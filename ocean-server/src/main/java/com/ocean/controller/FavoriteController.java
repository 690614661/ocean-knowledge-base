package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.common.PageResp;
import com.ocean.domain.dto.FavoriteResp;
import com.ocean.service.FavoriteService;
import com.ocean.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "用户收藏")
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    // ==================== 文档收藏 ====================

    @ApiOperation("切换文档收藏/取消收藏")
    @PostMapping("/toggle/{docId}")
    public CommonResp<Map<String, Boolean>> toggleDoc(@PathVariable Long docId, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        boolean favorited = favoriteService.toggleDoc(userId, docId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", favorited);
        String msg = favorited ? "收藏成功" : "已取消收藏";
        return CommonResp.ok(msg, result);
    }

    @ApiOperation("检查文档是否已收藏")
    @GetMapping("/check/{docId}")
    public CommonResp<Map<String, Boolean>> checkDoc(@PathVariable Long docId, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        boolean favorited = favoriteService.checkDoc(userId, docId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", favorited);
        return CommonResp.ok(result);
    }

    // ==================== 笔记收藏 ====================

    @ApiOperation("切换笔记收藏/取消收藏")
    @PostMapping("/note/toggle/{noteId}")
    public CommonResp<Map<String, Boolean>> toggleNote(@PathVariable Long noteId, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        boolean favorited = favoriteService.toggleNote(userId, noteId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", favorited);
        String msg = favorited ? "收藏成功" : "已取消收藏";
        return CommonResp.ok(msg, result);
    }

    @ApiOperation("检查笔记是否已收藏")
    @GetMapping("/note/check/{noteId}")
    public CommonResp<Map<String, Boolean>> checkNote(@PathVariable Long noteId, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        boolean favorited = favoriteService.checkNote(userId, noteId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", favorited);
        return CommonResp.ok(result);
    }

    // ==================== 收藏列表 ====================

    @ApiOperation("我的收藏列表")
    @GetMapping("/list")
    public CommonResp<PageResp<FavoriteResp>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        return CommonResp.ok(favoriteService.list(userId, page, size));
    }
}
