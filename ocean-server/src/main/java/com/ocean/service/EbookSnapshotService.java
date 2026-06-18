package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.Constant;
import com.ocean.domain.Ebook;
import com.ocean.domain.EbookSnapshot;
import com.ocean.domain.dto.StatisticResp;
import com.ocean.mapper.EbookSnapshotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EbookSnapshotService extends ServiceImpl<EbookSnapshotMapper, EbookSnapshot> {

    @Autowired
    private EbookService ebookService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void generateSnapshot() {
        List<Ebook> ebooks = ebookService.list();
        LocalDate today = LocalDate.now();

        for (Ebook ebook : ebooks) {
            // 查询今日是否已有快照
            EbookSnapshot todaySnapshot = this.getOne(new LambdaQueryWrapper<EbookSnapshot>()
                    .eq(EbookSnapshot::getEbookId, ebook.getId())
                    .eq(EbookSnapshot::getDate, today));

            // 查询昨日快照
            EbookSnapshot yesterdaySnapshot = this.getOne(new LambdaQueryWrapper<EbookSnapshot>()
                    .eq(EbookSnapshot::getEbookId, ebook.getId())
                    .eq(EbookSnapshot::getDate, today.minusDays(1)));

            int yesterdayViewCount = yesterdaySnapshot != null ? yesterdaySnapshot.getViewCount() : 0;
            int yesterdayVoteCount = yesterdaySnapshot != null ? yesterdaySnapshot.getVoteCount() : 0;

            if (todaySnapshot == null) {
                todaySnapshot = new EbookSnapshot();
                todaySnapshot.setEbookId(ebook.getId());
                todaySnapshot.setDate(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }
            todaySnapshot.setViewCount(ebook.getViewCount());
            todaySnapshot.setVoteCount(ebook.getVoteCount());
            todaySnapshot.setViewIncrease(ebook.getViewCount() - yesterdayViewCount);
            todaySnapshot.setVoteIncrease(ebook.getVoteCount() - yesterdayVoteCount);

            this.saveOrUpdate(todaySnapshot);
        }
    }

    public StatisticResp getStatistic() {
        StatisticResp resp = new StatisticResp();

        // 总量（从 doc 表实时读取）
        Map<String, Object> total = baseMapper.getTotalStatistic();
        int totalViewCount = total != null ? ((Number) total.getOrDefault("totalViewCount", 0)).intValue() : 0;
        int totalVoteCount = total != null ? ((Number) total.getOrDefault("totalVoteCount", 0)).intValue() : 0;

        resp.setTotalViewCount(totalViewCount);
        resp.setTotalVoteCount(totalVoteCount);
        resp.setVoteRate(totalViewCount > 0 ?
                BigDecimal.valueOf(totalVoteCount).divide(BigDecimal.valueOf(totalViewCount), 1, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0);

        // ===== 今日实时数据（Redis 实时计数器 + 快照保底） =====
        String todayStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer todayViewRedis = (Integer) redisTemplate.opsForValue().get(Constant.TODAY_VIEW_COUNT_PREFIX + todayStr);
        Integer todayVoteRedis = (Integer) redisTemplate.opsForValue().get(Constant.TODAY_VOTE_COUNT_PREFIX + todayStr);

        if (todayViewRedis == null) {
            // Redis 中没有（例如重启后），从快照表恢复
            Map<String, Object> todayMap = baseMapper.getTodayStatistic();
            todayViewRedis = todayMap != null ? ((Number) todayMap.getOrDefault("todayViewCount", 0)).intValue() : 0;
        }
        if (todayVoteRedis == null) {
            Map<String, Object> todayMap = baseMapper.getTodayStatistic();
            todayVoteRedis = todayMap != null ? ((Number) todayMap.getOrDefault("todayVoteCount", 0)).intValue() : 0;
        }

        resp.setTodayViewCount(todayViewRedis);
        resp.setTodayVoteCount(todayVoteRedis);

        // 预计今日阅读（按当前时间线性推算）
        int hourOfDay = java.time.LocalTime.now().getHour();
        if (hourOfDay > 0) {
            resp.setExpectedTodayViewCount(todayViewRedis * 24 / hourOfDay);
        } else {
            resp.setExpectedTodayViewCount(todayViewRedis);
        }

        // ===== 昨日增量 + 增长率 =====
        Map<String, Object> yesterday = baseMapper.getYesterdayIncrease();
        int yesterdayView = yesterday != null ? ((Number) yesterday.getOrDefault("viewIncrease", 0)).intValue() : 0;
        int yesterdayVote = yesterday != null ? ((Number) yesterday.getOrDefault("voteIncrease", 0)).intValue() : 0;

        resp.setViewIncreaseRate(yesterdayView > 0 ?
                BigDecimal.valueOf(todayViewRedis - yesterdayView).divide(BigDecimal.valueOf(yesterdayView), 1, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0);
        resp.setVoteIncreaseRate(yesterdayVote > 0 ?
                BigDecimal.valueOf(todayVoteRedis - yesterdayVote).divide(BigDecimal.valueOf(yesterdayVote), 1, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0);

        return resp;
    }

    public List<Map<String, Object>> get30DayTrend() {
        List<Map<String, Object>> trend = baseMapper.get30DayTrend();

        // 用 Redis 实时计数替换今日数据，让趋势图与卡片数据一致
        String todayStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer todayViewRedis = (Integer) redisTemplate.opsForValue().get(Constant.TODAY_VIEW_COUNT_PREFIX + todayStr);
        Integer todayVoteRedis = (Integer) redisTemplate.opsForValue().get(Constant.TODAY_VOTE_COUNT_PREFIX + todayStr);

        LocalDate today = LocalDate.now();
        String todayDateStr = today.toString();
        boolean foundToday = false;
        for (Map<String, Object> day : trend) {
            Object dateObj = day.get("date");
            String dateStr = dateObj != null ? dateObj.toString() : "";
            if (todayDateStr.equals(dateStr)) {
                foundToday = true;
                if (todayViewRedis != null) day.put("viewIncrease", todayViewRedis);
                if (todayVoteRedis != null) day.put("voteIncrease", todayVoteRedis);
                break;
            }
        }

        // 如果快照表中没有今天的数据，但 Redis 有，则追加
        if (!foundToday && (todayViewRedis != null && todayViewRedis > 0)) {
            Map<String, Object> todayEntry = new java.util.HashMap<String, Object>();
            todayEntry.put("date", todayDateStr);
            todayEntry.put("viewIncrease", todayViewRedis);
            todayEntry.put("voteIncrease", todayVoteRedis != null ? todayVoteRedis : 0);
            trend.add(todayEntry);
        }

        return trend;
    }
}
