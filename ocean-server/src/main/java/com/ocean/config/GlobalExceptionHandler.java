package com.ocean.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ocean.common.BusinessException;
import com.ocean.common.CommonResp;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public CommonResp<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return CommonResp.fail(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResp<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return CommonResp.fail("参数校验失败：" + message);
    }

    @ExceptionHandler(BindException.class)
    public CommonResp<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return CommonResp.fail("参数校验失败：" + message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResp<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", message);
        return CommonResp.fail("参数校验失败：" + message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public CommonResp<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return CommonResp.fail("请求方法不支持：" + e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public CommonResp<?> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的媒体类型: {}", e.getMessage());
        return CommonResp.fail("不支持的请求格式：" + e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public CommonResp<?> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return CommonResp.fail("请求参数格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public CommonResp<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getMessage());
        return CommonResp.fail("缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public CommonResp<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("数据完整性异常", e);
        return CommonResp.fail("操作失败，数据冲突");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResp<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return CommonResp.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public CommonResp<?> handleException(Exception e) {
        log.error("系统异常", e);
        return CommonResp.fail("系统异常，请稍后重试");
    }
}
