package com.ocean.controller;

import com.ocean.common.CommonResp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Api(tags = "全文检索")
@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUrl;

    @ApiOperation("全文检索")
    @GetMapping("")
    public CommonResp<Map<String, Object>> search(@RequestParam String keyword,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        try {
            // 构建 ES 查询
            Map<String, Object> queryBody = new HashMap<>();
            Map<String, Object> query = new HashMap<>();
            Map<String, Object> multiMatch = new HashMap<>();
            multiMatch.put("query", keyword);
            multiMatch.put("fields", Arrays.asList("name", "content"));
            query.put("multi_match", multiMatch);

            Map<String, Object> highlight = new HashMap<>();
            Map<String, Object> highlightFields = new HashMap<>();
            Map<String, Object> nameHl = new HashMap<>();
            nameHl.put("pre_tags", new String[]{"<em style='color:#dd4b39'>"});
            nameHl.put("post_tags", new String[]{"</em>"});
            Map<String, Object> contentHl = new HashMap<>();
            contentHl.put("pre_tags", new String[]{"<em style='color:#dd4b39'>"});
            contentHl.put("post_tags", new String[]{"</em>"});
            highlightFields.put("name", nameHl);
            highlightFields.put("content", contentHl);
            highlight.put("fields", highlightFields);

            queryBody.put("query", query);
            queryBody.put("highlight", highlight);
            queryBody.put("from", (page - 1) * size);
            queryBody.put("size", size);

            // 调用 ES HTTP API
            String esResponse = restTemplate.postForObject(
                    esUrl + "/doc_index/_search",
                    queryBody,
                    String.class);
            log.info("ES原始响应: {}", esResponse != null ? esResponse.substring(0, Math.min(esResponse.length(), 500)) : "null");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = com.alibaba.fastjson.JSON.parseObject(esResponse, Map.class);

            List<Map<String, Object>> resultList = new ArrayList<>();

            if (response != null) {
                Map<String, Object> hits = (Map<String, Object>) response.get("hits");
                if (hits != null) {
                    Map<String, Object> totalInfo = (Map<String, Object>) hits.get("total");
                    long total = totalInfo != null ? ((Number) totalInfo.get("value")).longValue() : 0;

                    List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");
                    if (hitList != null) {
                        for (Map<String, Object> hit : hitList) {
                            Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                            Map<String, Object> doc = new HashMap<>(source);
                            doc.put("id", hit.get("_id"));

                            // 替换高亮
                            Map<String, Object> hlFields = (Map<String, Object>) hit.get("highlight");
                            if (hlFields != null) {
                                if (hlFields.containsKey("name")) {
                                    List<String> hlName = (List<String>) hlFields.get("name");
                                    if (hlName != null && !hlName.isEmpty()) {
                                        doc.put("name", hlName.get(0));
                                    }
                                }
                                if (hlFields.containsKey("content")) {
                                    List<String> hlContent = (List<String>) hlFields.get("content");
                                    if (hlContent != null && !hlContent.isEmpty()) {
                                        doc.put("content", hlContent.get(0));
                                    }
                                }
                            }

                            resultList.add(doc);
                        }
                    }

                    Map<String, Object> result = new HashMap<>();
                    result.put("total", total);
                    result.put("list", resultList);

                    return CommonResp.ok(result);
                }
            }

            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("total", 0);
            emptyResult.put("list", resultList);
            return CommonResp.ok(emptyResult);
        } catch (Exception e) {
            log.warn("搜索服务暂不可用，返回空结果", e);
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("total", 0);
            emptyResult.put("list", new ArrayList<>());
            return CommonResp.ok(emptyResult);
        }
    }
}
