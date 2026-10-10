package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 凭证分类字典（对应表 receipt_category），首个页面用于导览展示 */
@Data
public class ReceiptCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    /** 适用场景 */
    private String scene;
    /** 特点说明 */
    private String feature;
    /** 识别关键词（逗号分隔），用于自动归类 */
    private String keywords;
    private Integer sort;
    /** 是否启用：1=启用 0=停用 */
    private Integer enabled;
    private LocalDateTime createTime;
}
