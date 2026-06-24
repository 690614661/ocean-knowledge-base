package com.ocean.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("favorite")
public class Favorite implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 目标ID（文档ID或笔记ID） */
    private Long docId;

    /** 收藏类型：1文档 2笔记 */
    private Integer targetType;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
