package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.common.Constant;
import com.ocean.common.CommonResp;
import com.ocean.common.PageResp;
import com.ocean.domain.User;
import com.ocean.domain.dto.ResetPasswordReq;
import com.ocean.domain.dto.UserLoginReq;
import com.ocean.domain.dto.UserSaveReq;
import com.ocean.mapper.UserMapper;
import com.ocean.util.JwtUtil;
import com.ocean.util.Md5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public CommonResp<Object> login(UserLoginReq req) {
        // 查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getLoginName, req.getLoginName()));
        if (user == null) {
            return CommonResp.fail("登录名或密码错误");
        }

        // 校验密码
        String encryptedPassword = Md5Util.encrypt(req.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            return CommonResp.fail("登录名或密码错误");
        }

        // 生成 JWT Token
        String token = JwtUtil.generateToken(user.getId(), user.getLoginName(), user.getName());

        // 存入 Redis
        redisTemplate.opsForValue().set(Constant.TOKEN_REDIS_PREFIX + user.getId(), token, 24, TimeUnit.HOURS);

        // 返回登录信息
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", token);
        loginInfo.put("userId", user.getId());
        loginInfo.put("loginName", user.getLoginName());
        loginInfo.put("name", user.getName());
        loginInfo.put("role", user.getRole());

        return CommonResp.ok("登录成功", loginInfo);
    }

    public CommonResp<Object> logout(String token) {
        Long userId = JwtUtil.getUserIdFromToken(token);
        redisTemplate.delete(Constant.TOKEN_REDIS_PREFIX + userId);
        return CommonResp.ok("退出成功");
    }

    public PageResp<User> list(int page, int size, String loginName) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(loginName)) {
            wrapper.like(User::getLoginName, loginName);
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> userPage = this.page(new Page<>(page, size), wrapper);

        // 清除密码字段
        userPage.getRecords().forEach(u -> u.setPassword(null));

        return new PageResp<>(userPage.getTotal(), userPage.getRecords());
    }

    public void save(UserSaveReq req) {
        if (req.getId() == null) {
            // 新增
            // 检查登录名唯一
            long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getLoginName, req.getLoginName()));
            if (count > 0) {
                throw new BusinessException("登录名已存在");
            }

            User user = new User();
            user.setLoginName(req.getLoginName());
            user.setName(req.getName());
            user.setRole(req.getRole() != null ? req.getRole() : "user");
            if (StringUtils.hasText(req.getPassword())) {
                user.setPassword(Md5Util.encrypt(req.getPassword()));
            }
            this.save(user);
        } else {
            // 编辑
            User user = this.getById(req.getId());
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            user.setName(req.getName());
            if (req.getRole() != null) {
                user.setRole(req.getRole());
            }
            if (StringUtils.hasText(req.getPassword())) {
                user.setPassword(Md5Util.encrypt(req.getPassword()));
            }
            this.updateById(user);
        }
    }

    public void resetPassword(ResetPasswordReq req) {
        User user = this.getById(req.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(Md5Util.encrypt(req.getPassword()));
        this.updateById(user);
    }

    public void delete(Long id) {
        this.removeById(id);
    }
}
