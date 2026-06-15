package com.ocean.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CategorySaveReq {

    private Long id;

    @NotNull(message = "父分类不能为空")
    private Long parent;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 20, message = "分类名称最大20字符")
    private String name;

    private Integer sort;
}
