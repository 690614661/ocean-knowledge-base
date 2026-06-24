package com.ocean.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private String type;

    private String title;

    private String content;

    private Long relatedId;

    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
