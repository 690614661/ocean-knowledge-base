package com.ocean.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class DocSaveReq {

    private Long id;

    @NotNull(message = "电子书ID不能为空")
    private Long ebookId;

    private Long parent;

    @NotBlank(message = "文档名称不能为空")
    @Size(max = 50, message = "文档名称最大50字符")
    private String name;

    private Integer sort;

    private String content;
}
