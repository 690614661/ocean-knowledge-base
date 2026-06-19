package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.domain.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId} AND doc_id = #{docId}")
    int countByUserAndDoc(@Param("userId") Long userId, @Param("docId") Long docId);
}
