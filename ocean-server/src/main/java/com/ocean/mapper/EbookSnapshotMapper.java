package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.domain.EbookSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface EbookSnapshotMapper extends BaseMapper<EbookSnapshot> {

    @Select("SELECT " +
            "IFNULL(SUM(view_count), 0) AS totalViewCount, " +
            "IFNULL(SUM(vote_count), 0) AS totalVoteCount " +
            "FROM doc")
    Map<String, Object> getTotalStatistic();

    @Select("SELECT " +
            "IFNULL(SUM(view_increase), 0) AS todayViewCount, " +
            "IFNULL(SUM(vote_increase), 0) AS todayVoteCount " +
            "FROM ebook_snapshot s " +
            "WHERE s.date = CURDATE()")
    Map<String, Object> getTodayStatistic();

    @Select("SELECT date, IFNULL(SUM(view_increase), 0) AS viewIncrease, IFNULL(SUM(vote_increase), 0) AS voteIncrease " +
            "FROM ebook_snapshot " +
            "WHERE date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY date " +
            "ORDER BY date ASC")
    List<Map<String, Object>> get30DayTrend();

    @Select("SELECT IFNULL(SUM(view_increase), 0) AS viewIncrease " +
            "FROM ebook_snapshot " +
            "WHERE date = DATE_SUB(CURDATE(), INTERVAL 1 DAY)")
    Map<String, Object> getYesterdayViewIncrease();

    @Select("SELECT IFNULL(SUM(view_increase), 0) AS viewIncrease " +
            "FROM ebook_snapshot " +
            "WHERE date = DATE_SUB(CURDATE(), INTERVAL 1 DAY)")
    Integer getYesterdayViewIncreaseInt();

    @Select("SELECT " +
            "IFNULL(SUM(view_increase), 0) AS viewIncrease, " +
            "IFNULL(SUM(vote_increase), 0) AS voteIncrease " +
            "FROM ebook_snapshot " +
            "WHERE date = DATE_SUB(CURDATE(), INTERVAL 1 DAY)")
    Map<String, Object> getYesterdayIncrease();
}
