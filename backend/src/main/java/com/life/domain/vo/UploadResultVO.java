package com.life.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 凭证上传结果（去重 + 审核 + 归类 + 时间识别） */
@Data
public class UploadResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 完全重复（MD5 命中）→ 已拒绝，未入库 */
    private Boolean duplicate;
    /** 疑似重复（感知哈希接近）→ 已入库但提示用户确认 */
    private Boolean similar;
    /** 面向用户的提示文案 */
    private String message;

    /** 审核状态：0=待审核 1=通过 2=不通过 */
    private Integer auditStatus;
    private String auditMsg;

    private Long categoryId;
    private String categoryName;

    private BigDecimal payAmount;
    private LocalDateTime payTime;
    /** 是否需要在弹窗中手动补录时间 */
    private Boolean needTime;
}
