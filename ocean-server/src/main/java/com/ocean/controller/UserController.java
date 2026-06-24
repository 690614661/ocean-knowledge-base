package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.common.PageResp;
import com.ocean.domain.User;
import com.ocean.domain.UserLoginLog;
import com.ocean.domain.dto.BatchDeleteReq;
import com.ocean.domain.dto.ChangePasswordReq;
import com.ocean.domain.dto.ResetPasswordReq;
import com.ocean.domain.dto.UserLoginReq;
import com.ocean.domain.dto.UserProfileUpdateReq;
import com.ocean.domain.dto.UserRegisterReq;
import com.ocean.domain.dto.UserSaveReq;
import com.ocean.interceptor.RateLimit;
import com.ocean.service.UserService;
import com.ocean.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @ApiOperation("用户登录")
    @RateLimit(permitsPerMinute = 5, permitsPerDay = 50)
    @PostMapping("/login")
    public CommonResp<Object> login(@Validated @RequestBody UserLoginReq req) {
        return userService.login(req);
    }

    @ApiOperation("用户退出")
    @GetMapping("/logout")
    public CommonResp<Object> logout(HttpServletRequest request) {
        String token = request.getHeader("token");
        return userService.logout(token);
    }

    @ApiOperation("用户注册")
    @RateLimit(permitsPerMinute = 3, permitsPerDay = 10)
    @PostMapping("/register")
    public CommonResp<Object> register(@Validated @RequestBody UserRegisterReq req) {
        return userService.register(req);
    }

    @ApiOperation("发送邮箱验证码")
    @RateLimit(permitsPerMinute = 5)
    @PostMapping("/send-code")
    public CommonResp<?> sendCode(@RequestParam String email) {
        userService.sendEmailCode(email);
        return CommonResp.ok("验证码已发送");
    }

    @ApiOperation("用户列表")
    @GetMapping("/list")
    public CommonResp<PageResp<User>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String loginName) {
        return CommonResp.ok(userService.list(page, size, loginName));
    }

    @ApiOperation("新增/编辑用户")
    @PostMapping("/save")
    public CommonResp<?> save(@Validated @RequestBody UserSaveReq req) {
        userService.save(req);
        return CommonResp.ok("保存成功");
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/delete/{id}")
    public CommonResp<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return CommonResp.ok("删除成功");
    }

    @ApiOperation("批量删除用户")
    @PostMapping("/delete/batch")
    public CommonResp<?> deleteBatch(@Validated @RequestBody BatchDeleteReq req, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long currentUserId = JwtUtil.getUserIdFromToken(token);
        // 校验不能删除自己
        for (Long id : req.getIds()) {
            if (id.equals(currentUserId)) {
                return CommonResp.fail("不能删除当前登录用户");
            }
        }
        userService.deleteBatch(req.getIds());
        return CommonResp.ok("批量删除成功");
    }

    @ApiOperation("重置密码")
    @PostMapping("/reset-password")
    public CommonResp<?> resetPassword(@Validated @RequestBody ResetPasswordReq req) {
        userService.resetPassword(req);
        return CommonResp.ok("密码重置成功");
    }

    @ApiOperation("获取个人信息")
    @GetMapping("/profile")
    public CommonResp<User> profile(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        return CommonResp.ok(userService.getProfile(userId));
    }

    @ApiOperation("更新个人信息")
    @PostMapping("/profile")
    public CommonResp<?> updateProfile(@Validated @RequestBody UserProfileUpdateReq req, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        userService.updateProfile(userId, req);
        return CommonResp.ok("保存成功");
    }

    @ApiOperation("修改密码")
    @PostMapping("/change-password")
    public CommonResp<?> changePassword(@Validated @RequestBody ChangePasswordReq req, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        userService.changePassword(userId, req);
        return CommonResp.ok("密码修改成功");
    }

    @ApiOperation("阅读历史")
    @GetMapping("/history")
    public CommonResp<PageResp<Map<String, Object>>> history(@RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size,
                                                              HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        return CommonResp.ok(userService.getHistory(userId, page, size));
    }

    @ApiOperation("登录日志（管理员）")
    @GetMapping("/login-log")
    public CommonResp<PageResp<UserLoginLog>> loginLog(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return CommonResp.ok(userService.getLoginLogs(page, size));
    }

    @ApiOperation("当前在线用户列表（管理员）")
    @GetMapping("/online")
    public CommonResp<Map<String, Object>> online() {
        Map<String, Object> result = new HashMap<>();
        result.put("count", userService.getOnlineCount());
        result.put("users", userService.getOnlineUsers());
        return CommonResp.ok(result);
    }
}
