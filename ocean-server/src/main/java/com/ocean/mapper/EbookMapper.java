package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.domain.Ebook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EbookMapper extends BaseMapper<Ebook> {

    @Select("SELECT e.*, " +
            "c1.name AS category1_name, c2.name AS category2_name " +
            "FROM ebook e " +
            "LEFT JOIN category c1 ON e.category1_id = c1.id " +
            "LEFT JOIN category c2 ON e.category2_id = c2.id " +
            "ORDER BY e.create_time DESC")
    List<Ebook> listWithCategory();
}
