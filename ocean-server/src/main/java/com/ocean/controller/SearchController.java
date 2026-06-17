package com.ocean.controller;

import com.ocean.common.CommonResp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "全文检索")
@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private RestHighLevelClient esClient;

    @ApiOperation("全文检索")
    @GetMapping("")
    public CommonResp<Map<String, Object>> search(@RequestParam String keyword,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        try {
            SearchRequest searchRequest = new SearchRequest("doc_index");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "name", "content"));
            sourceBuilder.from((page - 1) * size);
            sourceBuilder.size(size);

            // 高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("name");
            highlightBuilder.field("content");
            highlightBuilder.preTags("<em style='color:#dd4b39'>");
            highlightBuilder.postTags("</em>");
            sourceBuilder.highlighter(highlightBuilder);

            searchRequest.source(sourceBuilder);

            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (SearchHit hit : response.getHits()) {
                Map<String, Object> doc = new HashMap<>(hit.getSourceAsMap());
                doc.put("id", hit.getId());

                // 替换高亮
                if (hit.getHighlightFields().containsKey("name")) {
                    doc.put("name", hit.getHighlightFields().get("name").fragments()[0].string());
                }
                if (hit.getHighlightFields().containsKey("content")) {
                    String content = hit.getHighlightFields().get("content").fragments()[0].string();
                    doc.put("content", content);
                }

                resultList.add(doc);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", response.getHits().getTotalHits().value);
            result.put("list", resultList);

            return CommonResp.ok(result);
        } catch (Exception e) {
            log.error("搜索失败", e);
            return CommonResp.fail("搜索服务暂时不可用");
        }
    }
}
