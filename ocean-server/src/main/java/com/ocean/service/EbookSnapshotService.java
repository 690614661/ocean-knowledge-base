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
        // 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(Constant.CACHE_STATISTIC);
        if (cached instanceof StatisticResp) {
            return (StatisticResp) cached;
        }

        StatisticResp resp = new StatisticResp();

        // 总量
        Map<String, Object> total = baseMapper.getTotalStatistic();
        int totalViewCount = total != null ? ((Number) total.getOrDefault("totalViewCount", 0)).intValue() : 0;
        int totalVoteCount = total != null ? ((Number) total.getOrDefault("totalVoteCount", 0)).intValue() : 0;

        resp.setTotalViewCount(totalViewCount);
        resp.setTotalVoteCount(totalVoteCount);
        resp.setVoteRate(totalViewCount > 0 ?
                BigDecimal.valueOf(totalVoteCount).divide(BigDecimal.valueOf(totalViewCount), 1, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0);

        // 今日
        Map<String, Object> today = baseMapper.getTodayStatistic();
        int todayViewCount = today != null ? ((Number) today.getOrDefault("todayViewCount", 0)).intValue() : 0;
        int todayVoteCount = today != null ? ((Number) today.getOrDefault("todayVoteCount", 0)).intValue() : 0;

        resp.setTodayViewCount(todayViewCount);
        resp.setTodayVoteCount(todayVoteCount);

        // 预计今日阅读（按当前时间线性推算）
        int hourOfDay = java.time.LocalTime.now().getHour();
        if (hourOfDay > 0) {
            resp.setExpectedTodayViewCount(todayViewCount * 24 / hourOfDay);
        } else {
            resp.setExpectedTodayViewCount(todayViewCount);
        }

        // 昨日增量
        Integer yesterdayViewIncrease = baseMapper.getYesterdayViewIncreaseInt();
        int yvi = yesterdayViewIncrease != null ? yesterdayViewIncrease : 0;
        resp.setViewIncreaseRate(yvi > 0 ?
                BigDecimal.valueOf(todayViewCount - yvi).divide(BigDecimal.valueOf(yvi), 1, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0);
        resp.setVoteIncreaseRate(0.0);

        // 缓存1分钟
        redisTemplate.opsForValue().set(Constant.CACHE_STATISTIC, resp, 1, java.util.concurrent.TimeUnit.MINUTES);

        return resp;
    }

    public List<Map<String, Object>> get30DayTrend() {
        return baseMapper.get30DayTrend();
    }
}
