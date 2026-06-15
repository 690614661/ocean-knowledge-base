package com.ocean.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResp<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private List<T> list;

    public PageResp() {
    }

    public PageResp(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }
}
