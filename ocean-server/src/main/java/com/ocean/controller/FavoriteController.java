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

    @ApiOperation("切换收藏/取消收藏")
    @PostMapping("/toggle/{docId}")
    public CommonResp<Map<String, Boolean>> toggle(@PathVariable Long docId, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        boolean favorited = favoriteService.toggle(userId, docId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", favorited);
        String msg = favorited ? "收藏成功" : "已取消收藏";
        return CommonResp.ok(msg, result);
    }

    @ApiOperation("我的收藏列表")
    @GetMapping("/list")
    public CommonResp<PageResp<FavoriteResp>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        return CommonResp.ok(favoriteService.list(userId, page, size));
    }

    @ApiOperation("检查是否已收藏")
    @GetMapping("/check/{docId}")
    public CommonResp<Map<String, Boolean>> check(@PathVariable Long docId, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        boolean favorited = favoriteService.check(userId, docId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", favorited);
        return CommonResp.ok(result);
    }
}
