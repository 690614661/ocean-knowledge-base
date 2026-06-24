package com.ocean.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class DocCommentSaveReq {

    @NotNull(message = "文档ID不能为空")
    private Long docId;

    private Long parentId;

    private Long replyToUserId;

    private String replyToUserName;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
