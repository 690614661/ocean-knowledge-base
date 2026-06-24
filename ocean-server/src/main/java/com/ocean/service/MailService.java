package com.ocean.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮件发送服务
 * <p>
 * 当 MAIL_USERNAME 未配置时，验证码将打印到控制台（方便开发调试）。
 * 上线后配置环境变量 MAIL_USERNAME / MAIL_PASSWORD 即可启用真实邮件发送。
 */
@Slf4j
@Service
public class MailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送邮箱验证码
     */
    public void sendVerificationCode(String to, String code) {
        String subject = "海洋生物知识库 - 邮箱验证码";
        String text = "您的验证码是: " + code + "\n\n" +
                "该验证码5分钟内有效，请勿泄露给他人。\n\n" +
                "—— 海洋生物知识库";

        if (mailSender != null && StringUtils.hasText(from)) {
            // 真实邮件发送
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                mailSender.send(message);
                log.info("验证码邮件发送成功: to={}", to);
            } catch (Exception e) {
                log.error("验证码邮件发送失败: to={}", to, e);
                // 发送失败时也打印到日志，方便调试
                log.warn("【验证码降级】发送给 {} 的验证码为: {}", to, code);
            }
        } else {
            // 邮件未配置，打印到日志
            log.warn("【邮件未配置】发送给 {} 的验证码为: {}", to, code);
        }
    }
}
