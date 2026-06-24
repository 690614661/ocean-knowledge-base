package com.ocean.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class StatisticResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalViewCount;
    private Integer totalVoteCount;
    private Double voteRate;
    private Integer todayViewCount;
    private Integer todayVoteCount;
    private Integer expectedTodayViewCount;
    private Double viewIncreaseRate;
    private Double voteIncreaseRate;
}
