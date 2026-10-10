package com.life.service;

import com.life.dao.OutfitImageDao;
import com.life.storage.FileStorage;
import com.life.util.ImageGenClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 穿搭效果图异步生成器。
 *
 * <p><b>不实现接口</b>：@Async 方法若写在「实现了接口的 ServiceImpl」里，
 * JDK 动态代理只暴露接口方法会导致启动报错（见 TodoScheduler 的说明）。
 * 因此抽成独立 @Component，由 {@code OutfitImageServiceImpl} 外部调用。</p>
 *
 * <p>方法内 try-catch 兜底：任何异常（生图失败/存储失败）都落库置 status=2，
 * 否则任务会永久停在「生成中」。</p>
 */
@Component
public class OutfitImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(OutfitImageGenerator.class);

    /** 生图默认宽高比：三视图横排 */
    private static final String ASPECT = "16:9";

    @Autowired
    private ImageGenClient imageGenClient;

    @Autowired
    private OutfitImageDao outfitImageDao;

    @Autowired
    private FileStorage fileStorage;

    /** 异步执行生图（outfitExecutor 线程池）；refImage 非空则走图生图锁定人物 */
    @Async("outfitExecutor")
    public void run(Long userId, Long imageId, String prompt, byte[] refImage) {
        try {
            byte[] bytes = (refImage != null && refImage.length > 0)
                    ? imageGenClient.textToImageWithRef(prompt, ASPECT, refImage)
                    : imageGenClient.textToImage(prompt, ASPECT);
            String key = "outfit/image/" + userId + "/" + imageId + ".png";
            fileStorage.save(key, bytes, "image/png");
            outfitImageDao.updateStatus(imageId, 1, key, null);
            log.info("[outfit-image] 生图成功 imageId={}, key={}", imageId, key);
        } catch (Exception e) {
            log.error("[outfit-image] 生图失败 imageId={}", imageId, e);
            outfitImageDao.updateStatus(imageId, 2, null, truncate(e.getMessage()));
        }
    }

    private String truncate(String msg) {
        if (msg == null) {
            return "未知错误";
        }
        return msg.length() > 490 ? msg.substring(0, 490) : msg;
    }
}
