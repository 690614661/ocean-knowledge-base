package com.ocean.config;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ElasticsearchInitConfig implements ApplicationRunner {

    private static final String INDEX_NAME = "doc_index";
    private static final int MAX_RETRIES = 15;
    private static final long RETRY_DELAY_MS = 3000;

    @Autowired
    private RestHighLevelClient esClient;

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 1; i <= MAX_RETRIES; i++) {
            try {
                boolean exists = esClient.indices().exists(new GetIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);
                if (!exists) {
                    CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);
                    request.settings(Settings.builder()
                            .put("index.number_of_shards", 1)
                            .put("index.number_of_replicas", 0)
                    );
                    request.mapping("{\n" +
                            "  \"properties\": {\n" +
                            "    \"name\":     { \"type\": \"text\", \"analyzer\": \"standard\" },\n" +
                            "    \"content\":  { \"type\": \"text\", \"analyzer\": \"standard\" },\n" +
                            "    \"ebookId\":  { \"type\": \"long\" }\n" +
                            "  }\n" +
                            "}", XContentType.JSON);
                    esClient.indices().create(request, RequestOptions.DEFAULT);
                    log.info("ES 索引 doc_index 自动创建成功 (第{}次尝试)", i);
                } else {
                    log.info("ES 索引 doc_index 已存在，跳过创建");
                }
                return;
            } catch (Exception e) {
                log.warn("ES 索引初始化第{}/{}次失败 (ES 可能未就绪): {}", i, MAX_RETRIES, e.getMessage());
                if (i < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("ES 索引初始化失败，已重试{}次仍无法连接", MAX_RETRIES);
    }
}
