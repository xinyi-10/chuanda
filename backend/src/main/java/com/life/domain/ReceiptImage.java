package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付凭证图片（对应表 receipt_image） */
@Data
public class ReceiptImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long categoryId;
    private String fileName;
    /** 存储相对路径 */
    private String filePath;
    private Long fileSize;
    /** 内容 MD5，精确判重 */
    private String md5;
    /** 感知哈希（aHash），近似判重 */
    private String phash;

    /** 审核状态：0=待审核 1=通过 2=不通过 */
    private Integer auditStatus;
    private String auditMsg;

    /** 识别出的支付金额 */
    private BigDecimal payAmount;
    /** 识别出的支付时间 */
    private LocalDateTime payTime;
    /** 时间来源：1=自动识别 2=手动补录 */
    private Integer timeSource;

    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /* ===== 非表字段 ===== */

    private String categoryName;
    /** 可访问 URL（前端预览用） */
    private String url;
    /** 是否需要补录时间（未识别到时间时为 true） */
    private Boolean needTime;
}
