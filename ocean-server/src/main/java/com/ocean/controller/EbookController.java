package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.common.PageResp;
import com.ocean.domain.Ebook;
import com.ocean.domain.dto.BatchDeleteReq;
import com.ocean.domain.dto.EbookSaveReq;
import com.ocean.service.EbookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "电子书管理")
@RestController
@RequestMapping("/api/ebook")
public class EbookController {

    @Autowired
    private EbookService ebookService;

    @ApiOperation("电子书列表")
    @GetMapping("/list")
    public CommonResp<PageResp<Ebook>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) Long category1Id,
                                             @RequestParam(required = false) Long category2Id) {
        return CommonResp.ok(ebookService.list(page, size, name, category1Id, category2Id));
    }

    @ApiOperation("新增/编辑电子书")
    @PostMapping("/save")
    public CommonResp<?> save(@Validated @RequestBody EbookSaveReq req) {
        ebookService.save(req);
        return CommonResp.ok("保存成功");
    }

    @ApiOperation("删除电子书")
    @DeleteMapping("/delete/{id}")
    public CommonResp<?> delete(@PathVariable Long id) {
        ebookService.delete(id);
        return CommonResp.ok("删除成功");
    }

    @ApiOperation("批量删除电子书")
    @PostMapping("/delete/batch")
    public CommonResp<?> deleteBatch(@Validated @RequestBody BatchDeleteReq req) {
        ebookService.deleteBatch(req.getIds());
        return CommonResp.ok("批量删除成功");
    }
}
