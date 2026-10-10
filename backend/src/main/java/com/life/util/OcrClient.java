package com.life.util;

import com.life.domain.vo.OcrResult;

/**
 * 凭证识别客户端（可插拔）。
 *
 * <p><b>默认实现</b>：{@link RuleBasedOcrClient} —— 基于文件名/规则兜底，
 * 保证无任何外部依赖也能把「上传 → 审核 → 归类 → 时间确认」整条链路跑通。</p>
 *
 * <p><b>接入真实 OCR</b>：新增一个实现类（如调用百度/腾讯/阿里云 OCR，
 * 或本地 Tesseract），标注 {@code @Primary} 或调整注入即可，
 * 调用方 {@code ReceiptService} 无需任何改动 —— 与「LLM 失败走规则兜底」同思路。</p>
 */
public interface OcrClient {

    /**
     * 识别凭证图片。
     *
     * @param content  图片字节（不依赖本地磁盘，适配对象存储）
     * @param fileName 原始文件名（兜底实现会用它做规则判断）
     * @return 识别结果（含支付成功标识、金额、时间、原始文本）
     */
    OcrResult recognize(byte[] content, String fileName);
}
