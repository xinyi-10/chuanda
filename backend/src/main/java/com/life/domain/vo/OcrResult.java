package com.life.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** OCR 识别结果（凭证图片） */
@Data
public class OcrResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** OCR 是否调用成功（失败时走降级：全部待人工确认） */
    private boolean success;

    /** 是否检测到「支付成功」类标识 */
    private boolean paySuccess;

    /** 识别出的支付金额 */
    private BigDecimal amount;

    /** 识别出的支付时间 */
    private LocalDateTime payTime;

    /** 自动判定的分类编码 */
    private String categoryCode;

    /** 识别到的原始文本（便于排障 / 展示） */
    private String rawText;
}
