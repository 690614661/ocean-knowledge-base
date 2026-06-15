package com.ocean.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Around("execution(* com.ocean.controller.*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String logId = MDC.get("LOG_ID");

        log.info("[{}] >>> {}#{} params={}", logId, className, methodName, Arrays.toString(joinPoint.getArgs()));

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[{}] <<< {}#{} exception={} elapsed={}ms", logId, className, methodName, e.getMessage(), elapsed);
            throw e;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 500) {
            log.warn("[{}] <<< {}#{} elapsed={}ms [SLOW]", logId, className, methodName, elapsed);
        } else {
            log.info("[{}] <<< {}#{} elapsed={}ms", logId, className, methodName, elapsed);
        }

        return result;
    }
}
