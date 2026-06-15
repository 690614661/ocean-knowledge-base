package com.ocean.job;

import com.ocean.service.EbookSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EbookSnapshotJob {

    @Autowired
    private EbookSnapshotService ebookSnapshotService;

    @Scheduled(cron = "0 * * * * ?")
    public void generateSnapshot() {
        log.info("开始生成电子书数据快照");
        long start = System.currentTimeMillis();
        try {
            ebookSnapshotService.generateSnapshot();
            log.info("电子书数据快照生成完成，耗时{}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("电子书数据快照生成失败", e);
        }
    }
}
