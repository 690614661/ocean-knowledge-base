package com.ocean.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.common.CommonResp;
import com.ocean.domain.Content;
import com.ocean.domain.Doc;
import com.ocean.mapper.ContentMapper;
import com.ocean.mapper.DocMapper;
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
import java.util.stream.Collectors;

@Api(tags = "全文检索")
@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired(required = false)
    private RestHighLevelClient esClient;

    @Autowired
    private DocMapper docMapper;

    @Autowired
    private ContentMapper contentMapper;

    @ApiOperation("全文检索")
    @GetMapping("")
    public CommonResp<Map<String, Object>> search(@RequestParam String keyword,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        // 优先使用 ES 搜索
        if (esClient != null) {
            try {
                return searchWithEs(keyword, page, size);
            } catch (Exception e) {
                log.warn("ES 搜索失败，降级到 MySQL 搜索: {}", e.getMessage());
            }
        }
        // 降级：MySQL LIKE 搜索
        return searchWithMysql(keyword, page, size);
    }

    private CommonResp<Map<String, Object>> searchWithEs(String keyword, int page, int size) throws Exception {
        SearchRequest searchRequest = new SearchRequest("doc_index");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "name", "content"));
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);

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
    }

    private CommonResp<Map<String, Object>> searchWithMysql(String keyword, int page, int size) {
        // 搜索文档名称
        List<Doc> docs = docMapper.selectList(
                new LambdaQueryWrapper<Doc>()
                        .like(Doc::getName, keyword)
                        .last("LIMIT " + size + " OFFSET " + ((page - 1) * size)));

        // 搜索文档内容（限制条数）
        List<Content> contents = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .like(Content::getContent, keyword)
                        .last("LIMIT " + size + " OFFSET " + ((page - 1) * size)));

        // 合并去重
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Doc doc : docs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", doc.getId().toString());
            item.put("name", doc.getName().replaceAll(keyword, "<em style='color:#dd4b39'>$0</em>"));
            item.put("ebookId", doc.getEbookId());
            item.put("content", "");
            resultList.add(item);
        }

        // 补充内容匹配的结果
        for (Content c : contents) {
            Doc doc = docMapper.selectById(c.getId());
            if (doc == null) continue;

            String docIdStr = doc.getId().toString();
            boolean alreadyAdded = resultList.stream().anyMatch(r -> docIdStr.equals(r.get("id")));
            if (alreadyAdded) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("id", docIdStr);
            item.put("name", doc.getName());
            item.put("ebookId", doc.getEbookId());
            String snippet = c.getContent().length() > 200
                    ? c.getContent().substring(0, 200) + "..."
                    : c.getContent();
            item.put("content", snippet.replaceAll(keyword, "<em style='color:#dd4b39'>$0</em>"));
            resultList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", (long) resultList.size());
        result.put("list", resultList);

        return CommonResp.ok(result);
    }
}
