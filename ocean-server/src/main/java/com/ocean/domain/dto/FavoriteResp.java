package com.ocean.domain.dto;

import lombok.Data;

import java.util.Date;

@Data
public class FavoriteResp {

    private Long id;

    private Long docId;

    private String docName;

    private Long ebookId;

    private String ebookName;

    private Date createTime;
}
