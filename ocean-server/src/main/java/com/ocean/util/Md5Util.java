package com.ocean.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;

@Component
public class Md5Util {

    @Value("${salt.password}")
    private String passwordSalt;

    private static String PASSWORD_SALT;

    @PostConstruct
    public void init() {
        PASSWORD_SALT = passwordSalt;
    }

    public static String encrypt(String plaintext) {
        return DigestUtils.md5DigestAsHex((plaintext + PASSWORD_SALT).getBytes());
    }
}
