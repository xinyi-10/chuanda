package com.life.service;

import com.life.common.PageResult;
import com.life.domain.LedgerCategory;
import com.life.domain.LedgerRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 记账服务（流水 + 分类） */
public interface LedgerService {

    /**
     * 按来源入账（幂等）：凭证确认 / 工资自动入账共用。
     *
     * <p>若同一 (sourceType, sourceId) 已有流水，则直接返回其 ID，不重复插入。
     * 收支方向由 {@code categoryId} 对应分类的 type 派生（H6）。</p>
     *
     * @param sourceType 2=凭证入账 3=工资自动入账
     * @param sourceId   凭证 ID / 工资 YYYYMM
     * @return 流水 ID
     */
    Long postBySource(Long userId, Long categoryId, BigDecimal amount, LocalDateTime time,
                      String remark, Integer sourceType, Long sourceId);

    /* ===== 流水 ===== */

    PageResult<LedgerRecord> pageRecord(Long userId, Integer type, Long categoryId,
                                        String startDate, String endDate,
                                        Integer pageNum, Integer pageSize);

    LedgerRecord getRecord(Long id, Long userId);

    Long createRecord(Long userId, LedgerRecord record);

    void updateRecord(Long userId, LedgerRecord record);

    void deleteRecord(Long userId, Long id);

    /* ===== 分类 ===== */

    List<LedgerCategory> listCategory(Long userId, Integer type);

    /**
     * 初始化默认收支分类（幂等：已有分类则跳过）。
     * 注册时调用；同时作为「分类查询」的懒加载兜底，保证老用户也不会空。
     */
    void initDefaultCategories(Long userId);

    Long createCategory(Long userId, LedgerCategory category);

    void updateCategory(Long userId, LedgerCategory category);

    void deleteCategory(Long userId, Long id);
}
