package com.ocean.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ElasticsearchInitConfig implements ApplicationRunner {

    private static final String DOC_INDEX = "doc_index";
    private static final String NOTE_INDEX = "note_index";
    private static final int MAX_RETRIES = 15;
    private static final long RETRY_DELAY_MS = 3000;

    private final RestTemplate restTemplate;
    private final String esUrl;

    public ElasticsearchInitConfig(RestTemplate restTemplate,
                                   @Value("${spring.elasticsearch.uris:http://localhost:9200}") String esUrl) {
        this.restTemplate = restTemplate;
        this.esUrl = esUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 1; i <= MAX_RETRIES; i++) {
            try {
                initIndex(DOC_INDEX, getDocIndexMapping());
                initIndex(NOTE_INDEX, getNoteIndexMapping());
                log.info("ES 索引初始化完成 (doc_index, note_index)");
                return;
            } catch (Exception e) {
                log.warn("ES 索引初始化第{}/{}次失败: {}", i, MAX_RETRIES, e.getMessage());
                if (i < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("ES 索引初始化失败，已重试{}次仍无法连接", MAX_RETRIES);
    }

    private void initIndex(String indexName, String mapping) {
        try {
            restTemplate.getForObject(esUrl + "/" + indexName, String.class);
            log.info("ES 索引 {} 已存在，跳过创建", indexName);
        } catch (Exception ex) {
            // 索引不存在（404），创建
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(mapping, headers);

            restTemplate.exchange(esUrl + "/" + indexName, HttpMethod.PUT, entity, String.class);
            log.info("ES 索引 {} 自动创建成功", indexName);
        }
    }

    private String getDocIndexMapping() {
        return "{\n" +
                "  \"settings\": {\n" +
                "    \"number_of_shards\": 1,\n" +
                "    \"number_of_replicas\": 0\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "    \"properties\": {\n" +
                "      \"name\":       { \"type\": \"text\" },\n" +
                "      \"content\":    { \"type\": \"text\" },\n" +
                "      \"ebookId\":    { \"type\": \"long\" },\n" +
                "      \"createTime\": { \"type\": \"date\", \"format\": \"epoch_millis\" },\n" +
                "      \"viewCount\":  { \"type\": \"integer\" }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String getNoteIndexMapping() {
        return "{\n" +
                "  \"settings\": {\n" +
                "    \"number_of_shards\": 1,\n" +
                "    \"number_of_replicas\": 0\n" +
                "  },\n" +
                "  \"mappings\": {\n" +
                "    \"properties\": {\n" +
                "      \"title\":      { \"type\": \"text\" },\n" +
                "      \"content\":    { \"type\": \"text\" },\n" +
                "      \"userId\":     { \"type\": \"long\" },\n" +
                "      \"isPublic\":   { \"type\": \"byte\" },\n" +
                "      \"createTime\": { \"type\": \"date\", \"format\": \"epoch_millis\" },\n" +
                "      \"viewCount\":  { \"type\": \"integer\" }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }
}
