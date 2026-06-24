package com.ocean.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.domain.Note;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 统一 ES 搜索服务
 * <p>
 * 集中管理所有 ES 索引的 CRUD 和搜索操作，避免搜索逻辑散落在各 Controller 和 Service 中。
 * 当前管理的索引：
 * - doc_index  : 文档（由 DocService 触发同步）
 * - note_index : 公开笔记（由 NoteService 触发同步）
 * <p>
 * 索引同步使用 OkHttp 发送请求，避免 RestTemplate 内部编码转换导致中文乱码。
 */
@Slf4j
@Service
public class SearchService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUrl;

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public SearchService() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

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

    private SearchResult searchAll(String keyword, int page, int size) {
        // 各索引搜 size 条，合并后截取，保证两种类型都能出现
        int fetchSize = Math.max(size, 1);
        SearchResult docResult = searchSingleIndex("doc_index", keyword, 1, fetchSize,
                Arrays.asList("name", "content"));
        SearchResult noteResult = searchSingleIndex("note_index", keyword, 1, fetchSize,
                Arrays.asList("title", "content"));

        // 交叉合并，让文档和笔记交替出现，避免笔记被挤到后面
        List<Map<String, Object>> merged = new ArrayList<>();
        List<Map<String, Object>> docList = docResult.getList();
        List<Map<String, Object>> noteList = noteResult.getList();
        int maxLen = Math.max(docList.size(), noteList.size());
        for (int i = 0; i < maxLen; i++) {
            if (i < docList.size()) {
                docList.get(i).put("_index", "doc_index");
                merged.add(docList.get(i));
            }
            if (i < noteList.size()) {
                noteList.get(i).put("_index", "note_index");
                merged.add(noteList.get(i));
            }
        }

        int from = (page - 1) * size;
        List<Map<String, Object>> paged = merged.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());

        long total = docResult.getTotal() + noteResult.getTotal();
        return new SearchResult(total, paged);
    }

    // ==================== ES 查询构建 ====================

    private Map<String, Object> buildQuery(String keyword, int page, int size,
                                           List<String> fields, boolean isNoteIndex) {
        Map<String, Object> queryBody = new HashMap<>();

        Map<String, Object> query = new HashMap<>();
        Map<String, Object> multiMatch = new HashMap<>();
        multiMatch.put("query", keyword);
        multiMatch.put("fields", fields);
        query.put("multi_match", multiMatch);
        queryBody.put("query", query);

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

        queryBody.put("from", (page - 1) * size);
        queryBody.put("size", size);

        return queryBody;
    }

    // ==================== ES 响应解析 ====================

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

    // ==================== ES 索引同步（使用 OkHttp，UTF-8 编码可靠） ====================

    /**
     * 同步文档/笔记数据到 ES 索引
     * 使用 OkHttp 直发 UTF-8 JSON 字节，彻底避免编码问题
     */
    private void putToEs(String url, Map<String, Object> body) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(body);
            Request request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(jsonBytes, JSON_MEDIA_TYPE))
                    .build();
            okhttp3.Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.warn("ES PUT 返回非成功状态: url={}, code={}", url, response.code());
            }
            response.close();
        } catch (Exception e) {
            log.error("ES PUT 请求失败: url={}", url, e);
        }
    }

    // ==================== 文档索引同步 ====================

    public void syncDocIndex(Long docId, String name, String content, Long ebookId) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("name", name);
        docMap.put("content", content);
        docMap.put("ebookId", ebookId);
        putToEs(esUrl + "/doc_index/_doc/" + docId, docMap);
    }

    public void deleteDocIndex(Long docId) {
        try {
            restTemplate.delete(esUrl + "/doc_index/_doc/" + docId);
        } catch (Exception e) {
            log.error("文档索引删除失败: docId={}", docId, e);
        }
    }

    /**
     * 批量同步文档到 ES（用于重建索引）
     * @param docs 文档列表，每个元素需包含 id, name, content, ebookId 属性
     */
    public void syncDocBatch(List<Doc> docs) {
        int success = 0;
        for (Doc doc : docs) {
            try {
                String content = "";
                // 如果有content字段
                if (doc.getContent() != null) {
                    content = doc.getContent();
                }
                syncDocIndex(doc.getId(), doc.getName(), content, doc.getEbookId());
                success++;
            } catch (Exception e) {
                log.warn("批量同步文档失败: docId={}", doc.getId());
            }
        }
        log.info("批量同步文档完成: 共{}条, 成功{}条", docs.size(), success);
    }

    /**
     * 批量同步笔记到 ES（用于初始化或重建索引）
     */
    public void syncNoteBatch(List<Note> notes) {
        int success = 0;
        for (Note note : notes) {
            if (note.getIsPublic() == 1) {
                try {
                    syncNoteIndex(note.getId(), note.getTitle(), note.getContent(), note.getUserId(), true);
                    success++;
                } catch (Exception e) {
                    log.warn("批量同步笔记失败: noteId={}", note.getId());
                }
            }
        }
        log.info("批量同步笔记完成: 共{}条, 成功{}条", notes.size(), success);
    }

    // ==================== 笔记索引同步 ====================

    public void syncNoteIndex(Long noteId, String title, String content, Long userId, boolean isPublic) {
        try {
            if (!isPublic) {
                deleteNoteIndex(noteId);
                return;
            }

            Map<String, Object> noteMap = new HashMap<>();
            noteMap.put("title", title);
            noteMap.put("content", content != null ? content : "");
            noteMap.put("userId", userId);
            noteMap.put("isPublic", 1);

            putToEs(esUrl + "/note_index/_doc/" + noteId, noteMap);
        } catch (Exception e) {
            log.error("笔记索引同步失败: noteId={}", noteId, e);
        }
    }

    public void deleteNoteIndex(Long noteId) {
        try {
            restTemplate.delete(esUrl + "/note_index/_doc/" + noteId);
        } catch (Exception e) {
            log.error("笔记索引删除失败: noteId={}", noteId, e);
        }
    }
}
