package com.ocean.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.domain.Doc;
import com.ocean.domain.Ebook;
import com.ocean.domain.Note;
import com.ocean.mapper.DocMapper;
import com.ocean.mapper.NoteMapper;
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

    @Autowired
    private DocMapper docMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private EbookService ebookService;

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
        /** 分类ID，支持按分类过滤 */
        private Long categoryId;
        /** 电子书ID，支持按电子书过滤 */
        private Long ebookId;
        /** 排序方式：relevance / time / hot */
        private String sortBy = "relevance";
    }

    // ==================== 全文检索 ====================

    /**
     * 全文检索，支持搜索文档和公开笔记，支持过滤和排序
     */
    public SearchResult search(String keyword, int page, int size, SearchFilter filters) {
        String type = filters != null ? filters.getType() : "all";

        switch (type) {
            case "doc":
                return searchSingleIndex("doc_index", keyword, page, size,
                        Arrays.asList("name", "content"), filters);
            case "note":
                return searchSingleIndex("note_index", keyword, page, size,
                        Arrays.asList("title", "content"), filters);
            case "all":
            default:
                return searchAll(keyword, page, size, filters);
        }
    }

    private SearchResult searchSingleIndex(String indexName, String keyword, int page, int size,
                                           List<String> fields, SearchFilter filters) {
        try {
            Map<String, Object> queryBody = buildQuery(keyword, page, size, fields, indexName, filters);
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

    private SearchResult searchAll(String keyword, int page, int size, SearchFilter filters) {
        int fetchSize = Math.max(size, 1);

        // 如果指定了 ebookId，只搜 doc_index
        if (filters != null && filters.getEbookId() != null) {
            return searchSingleIndex("doc_index", keyword, page, size,
                    Arrays.asList("name", "content"), filters);
        }

        SearchResult docResult = searchSingleIndex("doc_index", keyword, 1, fetchSize,
                Arrays.asList("name", "content"), filters);
        SearchResult noteResult = searchSingleIndex("note_index", keyword, 1, fetchSize,
                Arrays.asList("title", "content"), filters);

        // 交叉合并，让文档和笔记交替出现
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
                                           List<String> fields, String indexName, SearchFilter filters) {
        Map<String, Object> queryBody = new HashMap<>();

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        // 构建 bool 查询（must + filter）
        Map<String, Object> boolQuery = new HashMap<>();
        List<Object> mustClauses = new ArrayList<>();

        // 关键词搜索
        if (hasKeyword) {
            Map<String, Object> multiMatch = new HashMap<>();
            multiMatch.put("query", keyword);
            multiMatch.put("fields", fields);
            Map<String, Object> mmQuery = new HashMap<>();
            mmQuery.put("multi_match", multiMatch);
            mustClauses.add(mmQuery);
        }

        // 没有 must 条件时用 match_all
        if (mustClauses.isEmpty()) {
            queryBody.put("query", Collections.singletonMap("match_all", Collections.emptyMap()));
        } else {
            boolQuery.put("must", mustClauses);
            queryBody.put("query", Collections.singletonMap("bool", boolQuery));
        }

        // 后置过滤器（不影响评分）
        if (filters != null) {
            List<Object> filterClauses = new ArrayList<>();

            // 按电子书过滤（仅 doc_index）
            if (filters.getEbookId() != null && "doc_index".equals(indexName)) {
                Map<String, Object> term = new HashMap<>();
                Map<String, Object> termField = new HashMap<>();
                termField.put("ebookId", filters.getEbookId());
                term.put("term", termField);
                filterClauses.add(term);
            }

            // 按分类过滤（仅 doc_index）
            if (filters.getCategoryId() != null && "doc_index".equals(indexName)) {
                List<Long> ebookIds = findEbookIdsByCategory(filters.getCategoryId());
                if (!ebookIds.isEmpty()) {
                    Map<String, Object> terms = new HashMap<>();
                    Map<String, Object> termsField = new HashMap<>();
                    termsField.put("ebookId", ebookIds);
                    terms.put("terms", termsField);
                    filterClauses.add(terms);
                }
            }

            if (!filterClauses.isEmpty()) {
                // 获取当前的 query 结构，将 filter 加入 bool
                Object currentQuery = queryBody.get("query");
                if (currentQuery instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> queryMap = (Map<String, Object>) currentQuery;
                    Object boolObj = queryMap.get("bool");
                    if (boolObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> bool = (Map<String, Object>) boolObj;
                        bool.put("filter", filterClauses);
                    } else {
                        // 当前是 match_all，包装成 bool
                        Map<String, Object> newBool = new HashMap<>();
                        newBool.put("must", Collections.singletonList(queryMap));
                        newBool.put("filter", filterClauses);
                        queryBody.put("query", Collections.singletonMap("bool", newBool));
                    }
                }
            }
        }

        // 高亮
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

        // 排序
        if (filters != null && filters.getSortBy() != null) {
            switch (filters.getSortBy()) {
                case "time":
                    List<Map<String, Object>> sortByTime = new ArrayList<>();
                    Map<String, Object> timeSort = new HashMap<>();
                    Map<String, Object> timeOrder = new HashMap<>();
                    timeOrder.put("order", "desc");
                    timeSort.put("createTime", timeOrder);
                    sortByTime.add(timeSort);
                    queryBody.put("sort", sortByTime);
                    break;
                case "hot":
                    List<Map<String, Object>> sortByHot = new ArrayList<>();
                    Map<String, Object> hotSort = new HashMap<>();
                    Map<String, Object> hotOrder = new HashMap<>();
                    hotOrder.put("order", "desc");
                    hotSort.put("viewCount", hotOrder);
                    sortByHot.add(hotSort);
                    queryBody.put("sort", sortByHot);
                    break;
                // relevance: 不传 sort 参数，ES 默认按 _score 排序
            }
        }

        queryBody.put("from", (page - 1) * size);
        queryBody.put("size", size);

        return queryBody;
    }

    /** 根据分类ID查找所有电子书ID（含一级和二级分类） */
    private List<Long> findEbookIdsByCategory(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        try {
            List<Ebook> ebooks = ebookService.list(
                    new LambdaQueryWrapper<Ebook>()
                            .eq(Ebook::getCategory1Id, categoryId)
                            .or()
                            .eq(Ebook::getCategory2Id, categoryId));
            for (Ebook ebook : ebooks) {
                ids.add(ebook.getId());
            }
        } catch (Exception e) {
            log.warn("按分类查询电子书失败: categoryId={}", categoryId, e);
        }
        return ids;
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
        // 补充排序字段
        Doc doc = docMapper.selectById(docId);
        if (doc != null) {
            if (doc.getCreateTime() != null) {
                docMap.put("createTime", doc.getCreateTime().getTime());
            }
            docMap.put("viewCount", doc.getViewCount() != null ? doc.getViewCount() : 0);
        }
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
     */
    public void syncDocBatch(List<Doc> docs) {
        int success = 0;
        for (Doc doc : docs) {
            try {
                String content = "";
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
            // 补充排序字段
            Note note = noteMapper.selectById(noteId);
            if (note != null) {
                if (note.getCreateTime() != null) {
                    noteMap.put("createTime", note.getCreateTime().getTime());
                }
                noteMap.put("viewCount", note.getViewCount() != null ? note.getViewCount() : 0);
            }

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
