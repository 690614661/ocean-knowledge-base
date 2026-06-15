package com.ocean.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ai_message")
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String conversationId;

    private String role;

    private String content;

    private Integer promptTokens;

    private Integer completionTokens;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
