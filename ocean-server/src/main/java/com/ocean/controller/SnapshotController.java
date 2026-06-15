package com.ocean.controller;

import com.ocean.common.CommonResp;
import com.ocean.domain.dto.StatisticResp;
import com.ocean.service.EbookSnapshotService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "数据统计")
@RestController
@RequestMapping("/api/snapshot")
public class SnapshotController {

    @Autowired
    private EbookSnapshotService ebookSnapshotService;

    @ApiOperation("获取统计数据")
    @GetMapping("/get-statistic")
    public CommonResp<Map<String, Object>> getStatistic() {
        StatisticResp statistic = ebookSnapshotService.getStatistic();
        List<Map<String, Object>> trend = ebookSnapshotService.get30DayTrend();

        Map<String, Object> result = new HashMap<>();
        result.put("totalViewCount", statistic.getTotalViewCount());
        result.put("totalVoteCount", statistic.getTotalVoteCount());
        result.put("voteRate", statistic.getVoteRate());
        result.put("todayViewCount", statistic.getTodayViewCount());
        result.put("todayVoteCount", statistic.getTodayVoteCount());
        result.put("expectedTodayViewCount", statistic.getExpectedTodayViewCount());
        result.put("viewIncreaseRate", statistic.getViewIncreaseRate());
        result.put("voteIncreaseRate", statistic.getVoteIncreaseRate());
        result.put("trendList", trend);

        return CommonResp.ok(result);
    }
}
