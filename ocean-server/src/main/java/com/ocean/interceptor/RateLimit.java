package com.ocean.interceptor;

import java.lang.annotation.*;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 每分钟允许的请求次数
     */
    int permitsPerMinute() default 10;

    /**
     * 每天允许的请求次数，-1 表示不限制
     */
    int permitsPerDay() default -1;
}
