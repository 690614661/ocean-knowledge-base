package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.common.Constant;
import com.ocean.common.CommonResp;
import com.ocean.common.PageResp;
import com.ocean.domain.Doc;
import com.ocean.domain.User;
import com.ocean.domain.dto.ChangePasswordReq;
import com.ocean.domain.dto.ResetPasswordReq;
import com.ocean.domain.dto.UserLoginReq;
import com.ocean.domain.dto.UserProfileUpdateReq;
import com.ocean.domain.dto.UserSaveReq;
import com.ocean.mapper.UserMapper;
import com.ocean.util.JwtUtil;
import com.ocean.util.Md5Util;
import com.ocean.util.BcryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        // 校验密码（BCrypt 优先，兼容旧 MD5）
        boolean passwordMatch;
        if (BcryptUtil.isBcrypt(user.getPassword())) {
            passwordMatch = BcryptUtil.matches(req.getPassword(), user.getPassword());
        } else {
            passwordMatch = Md5Util.encrypt(req.getPassword()).equals(user.getPassword());
            // 升级为 BCrypt
            if (passwordMatch) {
                user.setPassword(BcryptUtil.encrypt(req.getPassword()));
                this.updateById(user);
            }
        }
        if (!passwordMatch) {
            return CommonResp.fail("登录名或密码错误");
        }

        // 生成 JWT Token（含 role）
        String token = JwtUtil.generateToken(user.getId(), user.getLoginName(), user.getName(), user.getRole());

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
                user.setPassword(BcryptUtil.encrypt(req.getPassword()));
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
            user.setPassword(BcryptUtil.encrypt(req.getPassword()));
            }
            this.updateById(user);
        }
    }

    public void resetPassword(ResetPasswordReq req) {
        User user = this.getById(req.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(BcryptUtil.encrypt(req.getPassword()));
        this.updateById(user);
    }

    public void delete(Long id) {
        this.removeById(id);
    }

    public User getProfile(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public void updateProfile(Long userId, UserProfileUpdateReq req) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setName(req.getName());
        this.updateById(user);
    }

    public void changePassword(Long userId, ChangePasswordReq req) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码
        boolean passwordMatch;
        if (BcryptUtil.isBcrypt(user.getPassword())) {
            passwordMatch = BcryptUtil.matches(req.getOldPassword(), user.getPassword());
        } else {
            passwordMatch = Md5Util.encrypt(req.getOldPassword()).equals(user.getPassword());
        }
        if (!passwordMatch) {
            throw new BusinessException("原密码不正确");
        }

        // 设置新密码
        user.setPassword(BcryptUtil.encrypt(req.getNewPassword()));
        this.updateById(user);
    }

    @Autowired
    private DocService docService;

    public PageResp<Map<String, Object>> getHistory(Long userId, int page, int size) {
        String key = Constant.HISTORY_REDIS_PREFIX + userId;
        List<Object> docIds = redisTemplate.opsForList().range(key, 0, -1);

        if (docIds == null || docIds.isEmpty()) {
            return new PageResp<>(0, new ArrayList<>());
        }

        int total = docIds.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        if (fromIndex >= total) {
            return new PageResp<>(total, new ArrayList<>());
        }

        List<Object> pageIds = docIds.subList(fromIndex, toIndex);

        // 批量查询文档
        List<Long> ids = pageIds.stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
        List<Doc> docs = docService.listByIds(ids);
        Map<Long, Doc> docMap = docs.stream().collect(Collectors.toMap(Doc::getId, d -> d));

        // 保持 Redis 中的顺序（最新在前）
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : ids) {
            Doc doc = docMap.get(id);
            if (doc != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("docId", doc.getId());
                item.put("docName", doc.getName());
                item.put("ebookId", doc.getEbookId());
                result.add(item);
            }
        }

        return new PageResp<>(total, result);
    }
}
