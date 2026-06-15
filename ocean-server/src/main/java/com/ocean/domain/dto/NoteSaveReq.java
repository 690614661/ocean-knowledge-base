package com.ocean.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class NoteSaveReq {

    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最大100字符")
    private String title;

    private String content;

    private Integer isPublic;
}
