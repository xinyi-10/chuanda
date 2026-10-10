package com.life.util;

import com.life.domain.vo.ClothingRecognitionResult;

/**
 * 衣物图片识别客户端（可插拔）。
 *
 * <p>默认实现 {@link MiniMaxClothingRecognitionClient} 走 MiniMax 视觉模型
 * （MiniMax-VL-01），识别衣物的类别 / 季节 / 颜色 / 建议名称。
 * 识别失败时返回 {@code recognized=false}，由前端兜底让用户手动填写 ——
 * 与「天气失败降级」「LLM 失败走规则」同一套路。</p>
 */
public interface ClothingRecognitionClient {

    /**
     * 识别衣物图片。
     *
     * @param image 图片字节（JPG/PNG）
     * @return 识别结果；失败时 {@code recognized=false}
     */
    ClothingRecognitionResult recognize(byte[] image);
}
