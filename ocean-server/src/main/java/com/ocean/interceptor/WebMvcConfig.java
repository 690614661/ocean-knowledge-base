package com.ocean.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LogInterceptor logInterceptor;

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadPath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 日志拦截器 - 拦截所有请求
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");

        // 登录拦截器 - 拦截需要登录的接口
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/ebook/list",
                        "/api/category/list",
                        "/api/category/all",
                        "/api/doc/list",
                        "/api/doc/*",
                        "/api/snapshot/get-statistic",
                        "/api/user/login",
                        "/api/search",
                        "/api/note/public",
                        "/api/note/*",
                        "/files/**",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs/**",
                        "/error"
                );

        // 管理员拦截器 - 仅管理员可访问管理接口
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns(
                        "/api/ebook/save",
                        "/api/ebook/delete/**",
                        "/api/category/save",
                        "/api/category/delete/**",
                        "/api/doc/save",
                        "/api/doc/delete/**",
                        "/api/user/list",
                        "/api/user/save",
                        "/api/user/delete/**",
                        "/api/user/reset-password",
                        "/api/file/**"
                );
    }
}
