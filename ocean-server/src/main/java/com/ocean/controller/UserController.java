package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.common.PageResp;
import com.ocean.domain.User;
import com.ocean.domain.dto.ResetPasswordReq;
import com.ocean.domain.dto.UserLoginReq;
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

    @ApiOperation("重置密码")
    @PostMapping("/reset-password")
    public CommonResp<?> resetPassword(@Validated @RequestBody ResetPasswordReq req) {
        userService.resetPassword(req);
        return CommonResp.ok("密码重置成功");
    }
}
