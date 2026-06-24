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
import com.ocean.domain.UserLoginLog;
import com.ocean.domain.dto.*;
import com.ocean.mapper.UserLoginLogMapper;
import com.ocean.mapper.UserMapper;
import com.ocean.util.JwtUtil;
import com.ocean.util.Md5Util;
import com.ocean.util.BcryptUtil;
import com.ocean.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MailService mailService;

    @Autowired
    private DocService docService;

    @Autowired
    private UserLoginLogMapper userLoginLogMapper;

    // ==================== 登录/退出 ====================

    public CommonResp<Object> login(UserLoginReq req) {
        // 查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getLoginName, req.getLoginName()));
        if (user == null) {
            return CommonResp.fail("登录名或密码错误");
        }

        // 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            return CommonResp.fail("账户已被禁用，请联系管理员");
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

        // 记录登录日志
        try {
            UserLoginLog logEntry = new UserLoginLog();
            logEntry.setUserId(user.getId());
            logEntry.setLoginName(user.getLoginName());
            logEntry.setUserName(user.getName());
            logEntry.setIp(RequestUtil.getClientIp());
            userLoginLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }

        // 标记在线状态（Redis，30分钟超时）
        try {
            String onlineKey = Constant.ONLINE_USER_PREFIX + user.getId();
            Map<String, Object> onlineInfo = new HashMap<>();
            onlineInfo.put("userId", user.getId());
            onlineInfo.put("loginName", user.getLoginName());
            onlineInfo.put("name", user.getName());
            onlineInfo.put("role", user.getRole());
            onlineInfo.put("lastAccess", System.currentTimeMillis());
            redisTemplate.opsForValue().set(onlineKey, onlineInfo,
                    Constant.ONLINE_USER_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("标记在线状态失败", e);
        }

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
        // 清除在线状态
        try {
            redisTemplate.delete(Constant.ONLINE_USER_PREFIX + userId);
        } catch (Exception e) {
            log.error("清除在线状态失败", e);
        }
        return CommonResp.ok("退出成功");
    }

    // ==================== 注册 ====================

    /**
     * 发送邮箱验证码
     */
    public void sendEmailCode(String email) {
        // 1. 检查邮箱是否已被注册
        long exists = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (exists > 0) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 2. 检查频率限制（同一邮箱60秒内只能发一次）
        String rateLimitKey = "email:ratelimit:" + email;
        Boolean canSend = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, "1", 60, TimeUnit.SECONDS);
        if (canSend == null || !canSend) {
            throw new BusinessException("发送过于频繁，请60秒后再试");
        }

        // 3. 生成6位验证码，存Redis，5分钟有效
        String code = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set("email:code:" + email, code, 5, TimeUnit.MINUTES);

        // 4. 发送邮件
        mailService.sendVerificationCode(email, code);
    }

    /**
     * 用户自助注册
     */
    public CommonResp<Object> register(UserRegisterReq req) {
        // 1. 校验验证码
        String codeKey = "email:code:" + req.getEmail();
        String storedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            return CommonResp.fail("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(req.getEmailCode())) {
            return CommonResp.fail("验证码错误");
        }

        // 2. 校验登录名唯一性
        long loginNameExists = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getLoginName, req.getLoginName()));
        if (loginNameExists > 0) {
            // 验证码用过即废（避免暴力破解）
            redisTemplate.delete(codeKey);
            return CommonResp.fail("登录名已存在");
        }

        // 3. 校验邮箱唯一性（二次校验，防止并发）
        long emailExists = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, req.getEmail()));
        if (emailExists > 0) {
            redisTemplate.delete(codeKey);
            return CommonResp.fail("该邮箱已被注册");
        }

        // 4. 创建用户
        User user = new User();
        user.setLoginName(req.getLoginName());
        user.setName(req.getName());
        user.setPassword(BcryptUtil.encrypt(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setRole("user");
        user.setStatus(1);
        this.save(user);

        // 5. 删除已使用的验证码
        redisTemplate.delete(codeKey);

        // 6. 自动登录（返回token）
        String token = JwtUtil.generateToken(user.getId(), user.getLoginName(), user.getName(), user.getRole());
        redisTemplate.opsForValue().set(Constant.TOKEN_REDIS_PREFIX + user.getId(), token, 24, TimeUnit.HOURS);

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", token);
        loginInfo.put("userId", user.getId());
        loginInfo.put("loginName", user.getLoginName());
        loginInfo.put("name", user.getName());
        loginInfo.put("role", user.getRole());

        return CommonResp.ok("注册成功", loginInfo);
    }

    // ==================== 用户管理（管理员） ====================

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
            user.setStatus(1);
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

    /**
     * 批量删除用户
     */
    public void deleteBatch(List<Long> ids) {
        // 检查是否包含当前登录用户（由调用方传入 userId）
        this.removeByIds(ids);
    }

    // ==================== 个人中心 ====================

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
        if (req.getAvatar() != null) {
            user.setAvatar(req.getAvatar());
        }
        this.updateById(user);
    }

    public void changePassword(Long userId, ChangePasswordReq req) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        boolean passwordMatch;
        if (BcryptUtil.isBcrypt(user.getPassword())) {
            passwordMatch = BcryptUtil.matches(req.getOldPassword(), user.getPassword());
        } else {
            passwordMatch = Md5Util.encrypt(req.getOldPassword()).equals(user.getPassword());
        }
        if (!passwordMatch) {
            throw new BusinessException("原密码不正确");
        }

        user.setPassword(BcryptUtil.encrypt(req.getNewPassword()));
        this.updateById(user);
    }

    // ==================== 阅读历史 ====================

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

        List<Long> ids = pageIds.stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
        List<Doc> docs = docService.listByIds(ids);
        Map<Long, Doc> docMap = docs.stream().collect(Collectors.toMap(Doc::getId, d -> d));

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

    // ==================== 登录日志查询（管理员） ====================

    public PageResp<UserLoginLog> getLoginLogs(int page, int size) {
        IPage<UserLoginLog> logPage = userLoginLogMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<UserLoginLog>()
                        .orderByDesc(UserLoginLog::getLoginTime));
        return new PageResp<>(logPage.getTotal(), logPage.getRecords());
    }

    // ==================== 在线用户查询 ====================

    /**
     * 查询当前在线用户列表（Redis中所有 online:user:* 前缀的key）
     */
    public List<Map<String, Object>> getOnlineUsers() {
        List<Map<String, Object>> onlineUsers = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys(Constant.ONLINE_USER_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    Object obj = redisTemplate.opsForValue().get(key);
                    if (obj instanceof Map<?, ?>) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> info = (Map<String, Object>) obj;
                        onlineUsers.add(info);
                    }
                }
            }
            // 按最后访问时间倒序
            onlineUsers.sort((a, b) -> {
                Long aTime = (Long) a.getOrDefault("lastAccess", 0L);
                Long bTime = (Long) b.getOrDefault("lastAccess", 0L);
                return bTime.compareTo(aTime);
            });
        } catch (Exception e) {
            log.error("查询在线用户失败", e);
        }
        return onlineUsers;
    }

    /**
     * 获取当前在线人数
     */
    public long getOnlineCount() {
        try {
            Set<String> keys = redisTemplate.keys(Constant.ONLINE_USER_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("查询在线人数失败", e);
            return 0;
        }
    }
}
