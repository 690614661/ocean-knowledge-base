package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.domain.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId} AND doc_id = #{targetId} AND target_type = #{targetType}")
    int countByUserAndTarget(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("targetType") Integer targetType);
}
