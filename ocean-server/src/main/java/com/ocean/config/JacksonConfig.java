package com.ocean.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 全局配置
 * 将 Long 类型序列化为字符串，防止前端 JavaScript 丢失 Snowflake ID 精度
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        SimpleModule longToString = new SimpleModule();
        longToString.addSerializer(Long.class, ToStringSerializer.instance);
        longToString.addSerializer(Long.TYPE, ToStringSerializer.instance);

        return new Jackson2ObjectMapperBuilder()
                .modules(longToString)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
