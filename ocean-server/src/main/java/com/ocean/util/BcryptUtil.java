package com.ocean.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 密码加密工具（替代 MD5）
 */
@Component
public class BcryptUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 加密密码
     */
    public static String encrypt(String plaintext) {
        return ENCODER.encode(plaintext);
    }

    /**
     * 校验密码
     */
    public static boolean matches(String plaintext, String encoded) {
        return ENCODER.matches(plaintext, encoded);
    }

    /**
     * 判断是否为 BCrypt 格式
     */
    public static boolean isBcrypt(String encoded) {
        return encoded != null && encoded.startsWith("$2a$");
    }
}
