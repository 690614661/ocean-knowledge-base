package com.ocean.config;

import org.springframework.context.annotation.Configuration;

/**
 * ES 配置已迁移为 HTTP API 调用（兼容 ES 8+/9+）
 * 相关配置见 application.yml spring.elasticsearch.uris
 * RestTemplate bean 见 RestTemplateConfig
 */
@Configuration
public class ElasticsearchConfig {

}
