package com.ocean.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@TableName("doc_comment")
public class DocComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long docId;

    private Long parentId;

    private Long replyToUserId;

    private String replyToUserName;

    private Long userId;

    private String userName;

    private String content;

    private Date createTime;

    /** 子评论列表（非数据库字段） */
    @TableField(exist = false)
    private List<DocComment> children;

    /** 是否有子评论（非数据库字段） */
    @TableField(exist = false)
    private boolean hasChildren;
}
