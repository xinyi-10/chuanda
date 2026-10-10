package com.life.service;

import com.life.dao.OutfitImageDao;
import com.life.domain.OutfitImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 穿搭效果图超时补偿。
 *
 * <p>定时扫描 status=0（生成中）且已超过 3 分钟的任务，置为 status=3（超时终态），
 * 杜绝「永久生成中」。与 {@code AsyncConfig} 里 outfitExecutor 的背压策略呼应。</p>
 */
@Component
public class OutfitImageScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutfitImageScheduler.class);

    /** 生成中超过该分钟数即判超时 */
    private static final int TIMEOUT_MINUTES = 3;

    @Autowired
    private OutfitImageDao outfitImageDao;

    /** 每 2 分钟扫一次 */
    @Scheduled(fixedDelay = 120000, initialDelay = 60000)
    public void timeoutCompensate() {
        try {
            List<OutfitImage> list = outfitImageDao.listTimeout(TIMEOUT_MINUTES);
            for (OutfitImage img : list) {
                outfitImageDao.updateStatus(img.getId(), 3, null, "生成超时");
                log.warn("[outfit-image] 任务 {} 超时，已置为超时态", img.getId());
            }
        } catch (Exception e) {
            log.error("[outfit-image] 超时补偿扫描失败", e);
        }
    }
}
