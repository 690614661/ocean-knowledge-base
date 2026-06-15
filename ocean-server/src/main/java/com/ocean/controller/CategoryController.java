package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.domain.Category;
import com.ocean.domain.dto.CategorySaveReq;
import com.ocean.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "分类管理")
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @ApiOperation("分类树形列表")
    @GetMapping("/list")
    public CommonResp<List<Category>> tree() {
        return CommonResp.ok(categoryService.tree());
    }

    @ApiOperation("所有分类（扁平）")
    @GetMapping("/all")
    public CommonResp<List<Category>> all() {
        return CommonResp.ok(categoryService.all());
    }

    @ApiOperation("新增/编辑分类")
    @PostMapping("/save")
    public CommonResp<?> save(@Validated @RequestBody CategorySaveReq req) {
        categoryService.save(req);
        return CommonResp.ok("保存成功");
    }

    @ApiOperation("删除分类")
    @DeleteMapping("/delete/{id}")
    public CommonResp<?> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return CommonResp.ok("删除成功");
    }
}
