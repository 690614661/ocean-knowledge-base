package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.domain.DocComment;
import com.ocean.domain.dto.DocCommentSaveReq;
import com.ocean.service.DocCommentService;
import com.ocean.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(tags = "文档评论")
@RestController
@RequestMapping("/api/comment")
public class DocCommentController {

    @Autowired
    private DocCommentService docCommentService;

    @ApiOperation("获取文档评论列表")
    @GetMapping("/list/{docId}")
    public CommonResp<List<DocComment>> list(@PathVariable Long docId) {
        return CommonResp.ok(docCommentService.listByDocId(docId));
    }

    @ApiOperation("发表评论")
    @PostMapping("/save")
    public CommonResp<DocComment> save(@Validated @RequestBody DocCommentSaveReq req,
                                       HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        String userName = JwtUtil.getNameFromToken(token);
        return CommonResp.ok("评论成功", docCommentService.save(req, userId, userName));
    }

    @ApiOperation("删除评论")
    @DeleteMapping("/delete/{id}")
    public CommonResp<?> delete(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserIdFromToken(token);
        docCommentService.delete(id, userId);
        return CommonResp.ok("删除成功");
    }
}
