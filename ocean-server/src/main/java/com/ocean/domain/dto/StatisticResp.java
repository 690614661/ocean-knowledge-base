package com.ocean.domain.dto;

import lombok.Data;

@Data
public class StatisticResp {

    private Integer totalViewCount;
    private Integer totalVoteCount;
    private Double voteRate;
    private Integer todayViewCount;
    private Integer todayVoteCount;
    private Integer expectedTodayViewCount;
    private Double viewIncreaseRate;
    private Double voteIncreaseRate;
}
