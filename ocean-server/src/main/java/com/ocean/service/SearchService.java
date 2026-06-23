package com.ocean.service;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一 ES 搜索服务
 * <p>
 * 集中管理所有 ES 索引的 CRUD 和搜索操作，避免搜索逻辑散落在各 Controller 和 Service 中。
 * 当前管理的索引：
 * - doc_index  : 文档（由 DocService 触发同步）
 * - note_index : 公开笔记（由 NoteService 触发同步）
 */
@Slf4j
@Service
public class SearchService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUrl;

    // ==================== 搜索结果封装 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private long total;
        private List<Map<String, Object>> list;
    }

    @Data
    public static class SearchFilter {
        /** 搜索类型：all / doc / note */
        private String type = "all";
        /** 分类ID（预留，后续支持按分类过滤） */
        private Long categoryId;
        /** 电子书ID（预留，后续支持按电子书过滤） */
        private Long ebookId;
        /** 排序方式：relevance / time / hot（预留） */
        private String sortBy = "relevance";
    }

    // ==================== 全文检索 ====================

    /**
     * 全文检索，支持搜索文档和公开笔记
     *
     * @param keyword 搜索关键词
     * @param page    页码（从1开始）
     * @param size    每页条数
     * @param filters 过滤条件
     * @return 搜索结果
     */
    public SearchResult search(String keyword, int page, int size, SearchFilter filters) {
        String type = filters != null ? filters.getType() : "all";

        switch (type) {
            case "doc":
                return searchSingleIndex("doc_index", keyword, page, size,
                        Arrays.asList("name", "content"));
            case "note":
                return searchSingleIndex("note_index", keyword, page, size,
                        Arrays.asList("title", "content"));
            case "all":
            default:
                return searchAll(keyword, page, size);
        }
    }

    /**
     * 搜索单个索引
     */
    private SearchResult searchSingleIndex(String indexName, String keyword, int page, int size,
                                           List<String> fields) {
        try {
            Map<String, Object> queryBody = buildQuery(keyword, page, size, fields, indexName.equals("note_index"));
            String esResponse = restTemplate.postForObject(
                    esUrl + "/" + indexName + "/_search",
                    queryBody,
                    String.class);

            return parseResponse(esResponse, indexName);
        } catch (Exception e) {
            log.warn("搜索 {} 暂不可用: {}", indexName, e.getMessage());
            return new SearchResult(0, new ArrayList<>());
        }
    }

    /**
     * 同时搜索文档 + 笔记，合并结果按相关性混排
     */
    private SearchResult searchAll(String keyword, int page, int size) {
        // 文档和笔记各搜一半，合并后截取
        int halfSize = Math.max(size / 2, size);
        SearchResult docResult = searchSingleIndex("doc_index", keyword, 1, halfSize,
                Arrays.asList("name", "content"));
        SearchResult noteResult = searchSingleIndex("note_index", keyword, 1, halfSize,
                Arrays.asList("title", "content"));

        // 合并并标记来源
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> doc : docResult.getList()) {
            doc.put("_index", "doc_index");
            merged.add(doc);
        }
        for (Map<String, Object> note : noteResult.getList()) {
            note.put("_index", "note_index");
            merged.add(note);
        }

        // 按总分页截取
        int from = (page - 1) * size;
        List<Map<String, Object>> paged = merged.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());

        long total = docResult.getTotal() + noteResult.getTotal();
        return new SearchResult(total, paged);
    }

    // ==================== ES 查询构建 ====================

    /**
     * 构建 ES 搜索请求体
     */
    private Map<String, Object> buildQuery(String keyword, int page, int size,
                                           List<String> fields, boolean isNoteIndex) {
        Map<String, Object> queryBody = new HashMap<>();

        // multi_match 查询
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> multiMatch = new HashMap<>();
        multiMatch.put("query", keyword);
        multiMatch.put("fields", fields);
        query.put("multi_match", multiMatch);
        queryBody.put("query", query);

        // 高亮设置
        Map<String, Object> highlight = new HashMap<>();
        Map<String, Object> highlightFields = new HashMap<>();
        for (String field : fields) {
            Map<String, Object> hlConfig = new HashMap<>();
            hlConfig.put("pre_tags", new String[]{"<em style='color:#dd4b39'>"});
            hlConfig.put("post_tags", new String[]{"</em>"});
            highlightFields.put(field, hlConfig);
        }
        highlight.put("fields", highlightFields);
        queryBody.put("highlight", highlight);

        // 分页
        queryBody.put("from", (page - 1) * size);
        queryBody.put("size", size);

        return queryBody;
    }

    // ==================== ES 响应解析 ====================

    /**
     * 解析 ES 搜索响应
     */
    @SuppressWarnings("unchecked")
    private SearchResult parseResponse(String esResponse, String indexName) {
        if (esResponse == null) {
            return new SearchResult(0, new ArrayList<>());
        }

        Map<String, Object> response = JSON.parseObject(esResponse, Map.class);
        List<Map<String, Object>> resultList = new ArrayList<>();

        Map<String, Object> hits = (Map<String, Object>) response.get("hits");
        if (hits == null) {
            return new SearchResult(0, resultList);
        }

        Map<String, Object> totalInfo = (Map<String, Object>) hits.get("total");
        long total = totalInfo != null ? ((Number) totalInfo.get("value")).longValue() : 0;

        List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");
        if (hitList != null) {
            for (Map<String, Object> hit : hitList) {
                Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                Map<String, Object> doc = new HashMap<>(source);
                doc.put("id", hit.get("_id"));

                // 替换高亮内容
                Map<String, Object> hlFields = (Map<String, Object>) hit.get("highlight");
                if (hlFields != null) {
                    for (String field : new String[]{"name", "title", "content"}) {
                        if (hlFields.containsKey(field)) {
                            List<String> hlValues = (List<String>) hlFields.get(field);
                            if (hlValues != null && !hlValues.isEmpty()) {
                                doc.put(field, hlValues.get(0));
                            }
                        }
                    }
                }

                resultList.add(doc);
            }
        }

        return new SearchResult(total, resultList);
    }

    // ==================== 文档索引同步 ====================

    /**
     * 同步文档到 ES 索引
     */
    public void syncDocIndex(Long docId, String name, String content, Long ebookId) {
        try {
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("name", name);
            docMap.put("content", content);
            docMap.put("ebookId", ebookId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(docMap, headers);
            restTemplate.put(esUrl + "/doc_index/_doc/" + docId, entity);
        } catch (Exception e) {
            log.error("文档索引同步失败: docId={}", docId, e);
        }
    }

    /**
     * 从 ES 删除文档索引
     */
    public void deleteDocIndex(Long docId) {
        try {
            restTemplate.delete(esUrl + "/doc_index/_doc/" + docId);
        } catch (Exception e) {
            log.error("文档索引删除失败: docId={}", docId, e);
        }
    }

    // ==================== 笔记索引同步 ====================

    /**
     * 同步笔记到 ES 索引（仅公开笔记可被搜索）
     */
    public void syncNoteIndex(Long noteId, String title, String content, Long userId, boolean isPublic) {
        try {
            if (!isPublic) {
                // 私有笔记：确保从 ES 中删除（防止改私有后残留）
                deleteNoteIndex(noteId);
                return;
            }

            Map<String, Object> noteMap = new HashMap<>();
            noteMap.put("title", title);
            noteMap.put("content", content != null ? content : "");
            noteMap.put("userId", userId);
            noteMap.put("isPublic", 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(noteMap, headers);
            restTemplate.put(esUrl + "/note_index/_doc/" + noteId, entity);
        } catch (Exception e) {
            log.error("笔记索引同步失败: noteId={}", noteId, e);
        }
    }

    /**
     * 从 ES 删除笔记索引
     */
    public void deleteNoteIndex(Long noteId) {
        try {
            restTemplate.delete(esUrl + "/note_index/_doc/" + noteId);
        } catch (Exception e) {
            log.error("笔记索引删除失败: noteId={}", noteId, e);
        }
    }
}
