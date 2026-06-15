package com.ocean.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class EbookSaveReq {

    private Long id;

    @NotBlank(message = "名称不能为空")
    @Size(max = 50, message = "名称最大50字符")
    private String name;

    @NotNull(message = "一级分类必须选择")
    private Long category1Id;

    @NotNull(message = "二级分类必须选择")
    private Long category2Id;

    @Size(max = 200, message = "描述最大200字符")
    private String description;

    private String cover;
}
