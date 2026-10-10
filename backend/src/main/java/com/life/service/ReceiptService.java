package com.life.service;

import com.life.common.PageResult;
import com.life.domain.ReceiptCategory;
import com.life.domain.ReceiptImage;
import com.life.domain.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 支付凭证服务 */
public interface ReceiptService {

    /** 分类导览列表（首个页面展示适用场景 / 特点） */
    List<ReceiptCategory> listCategories();

    /**
     * 上传凭证：去重 → 落盘 → 审核 → 归类 → 时间识别。
     *
     * @return 上传结果（含重复标记、审核状态、归类、是否需补录时间）
     */
    UploadResultVO upload(Long userId, MultipartFile file);

    PageResult<ReceiptImage> page(Long userId, Long categoryId, Integer auditStatus,
                                  Integer pageNum, Integer pageSize);

    ReceiptImage getById(Long id, Long userId);

    /**
     * 确认凭证：补录 / 修正时间与金额，并**自动写入记账**（幂等，同一凭证只入账一次）。
     *
     * @param ledgerCategoryId 记账分类 ID —— 其 type 决定这笔是「支出」还是「收入」
     */
    ReceiptImage fillTime(Long userId, Long id, BigDecimal payAmount, LocalDateTime payTime,
                          String remark, Long ledgerCategoryId);

    /** 人工纠正自动归类结果 */
    ReceiptImage correctCategory(Long userId, Long id, Long categoryId);

    void delete(Long userId, Long id);
}
