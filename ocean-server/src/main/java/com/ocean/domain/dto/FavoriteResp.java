package com.ocean.domain.dto;

import lombok.Data;

import java.util.Date;

@Data
public class FavoriteResp {

    private Long id;

    private Long docId;

    /** 收藏类型：1文档 2笔记 */
    private Integer targetType;

    /** 文档/笔记名称 */
    private String name;

    /** 所属电子书ID（仅文档） */
    private Long ebookId;

    /** 所属电子书名称（仅文档） */
    private String ebookName;

    private Date createTime;
}
