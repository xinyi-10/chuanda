package com.life.util;

/**
 * 生图客户端（可插拔）。
 *
 * <p>默认实现 {@link MiniMaxImageGenClient} 调用 MiniMax image-01 模型。
 * 与 {@link WeatherClient} 同一套路：业务方只依赖本接口，切换生图服务
 * 只需替换实现类，调用方 {@code OutfitImageGenerator} 零改动。</p>
 */
public interface ImageGenClient {

    /**
     * 文生图。
     *
     * @param prompt      提示词
     * @param aspectRatio 宽高比（如 16:9 / 1:1 / 3:4）
     * @return 图片字节；失败抛异常（由调用方降级/记录）
     */
    byte[] textToImage(String prompt, String aspectRatio);

    /**
     * 图生图（角色一致性）：用参考图锁定人物主体，换场景/换装仍保持同一形象。
     *
     * @param prompt      提示词
     * @param aspectRatio 宽高比
     * @param refImage    参考图字节（建议单人正面照；实现类负责编码）
     * @return 图片字节；失败抛异常
     */
    byte[] textToImageWithRef(String prompt, String aspectRatio, byte[] refImage);
}
