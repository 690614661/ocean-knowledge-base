package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.service.SearchService;
import com.ocean.service.SearchService.SearchFilter;
import com.ocean.service.SearchService.SearchResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "全文检索")
@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @ApiOperation("全文检索")
    @GetMapping("")
    public CommonResp<Map<String, Object>> search(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(defaultValue = "all") String type,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) Long ebookId,
                                                  @RequestParam(defaultValue = "relevance") String sortBy) {
        SearchFilter filters = new SearchFilter();
        filters.setType(type);
        filters.setCategoryId(categoryId);
        filters.setEbookId(ebookId);
        filters.setSortBy(sortBy);

        keyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();
        SearchResult result = searchService.search(keyword, page, size, filters);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total", result.getTotal());
        resultMap.put("list", result.getList());
        return CommonResp.ok(resultMap);
    }
}
