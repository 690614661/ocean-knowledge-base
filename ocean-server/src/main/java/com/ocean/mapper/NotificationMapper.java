package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT COUNT(*) FROM notification WHERE to_user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);

    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id} AND to_user_id = #{userId}")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE notification SET is_read = 1 WHERE to_user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);
}
