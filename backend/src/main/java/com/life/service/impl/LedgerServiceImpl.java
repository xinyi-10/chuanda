package com.life.service.impl;

import com.life.common.BizException;
import com.life.common.PageResult;
import com.life.common.ResultCode;
import com.life.dao.LedgerCategoryDao;
import com.life.dao.LedgerRecordDao;
import com.life.domain.LedgerCategory;
import com.life.domain.LedgerRecord;
import com.life.service.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 记账服务实现（流水 + 分类） */
@Service
public class LedgerServiceImpl implements LedgerService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private LedgerRecordDao recordDao;

    @Autowired
    private LedgerCategoryDao categoryDao;

    /* ================= 流水 ================= */

    @Override
    public PageResult<LedgerRecord> pageRecord(Long userId, Integer type, Long categoryId,
                                               String startDate, String endDate,
                                               Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) ? 10 : pageSize;
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);

        long total = recordDao.countByCondition(userId, type, categoryId, start, end);
        List<LedgerRecord> list = recordDao.listByPage(userId, type, categoryId, start, end, (pn - 1) * ps, ps);
        return PageResult.of(list, total, pn, ps);
    }

    @Override
    public LedgerRecord getRecord(Long id, Long userId) {
        LedgerRecord record = recordDao.selectById(id, userId);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return record;
    }

    @Override
    public Long createRecord(Long userId, LedgerRecord record) {
        validateRecord(userId, record);
        if (record.getRecordTime() == null) {
            record.setRecordTime(LocalDateTime.now());
        }
        record.setUserId(userId);
        // 手动记账默认来源（source_type 为 NOT NULL，必须赋默认值）
        if (record.getSourceType() == null) {
            record.setSourceType(1);
        }
        recordDao.insert(record);
        return record.getId();
    }

    @Override
    public void updateRecord(Long userId, LedgerRecord record) {
        if (record.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "缺少流水 ID");
        }
        if (recordDao.selectById(record.getId(), userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        validateRecord(userId, record);
        record.setUserId(userId);
        recordDao.update(record);
    }

    @Override
    public void deleteRecord(Long userId, Long id) {
        recordDao.logicDelete(id, userId);
    }

    /* ================= 分类 ================= */

    @Override
    public List<LedgerCategory> listCategory(Long userId, Integer type) {
        // 懒加载兜底：新注册 / 老用户首次访问时补齐默认分类，避免「选不到分类无法记账」
        initDefaultCategories(userId);
        return categoryDao.listByUser(userId, type);
    }

    /** 默认支出分类：名称 / 图标标识 / 排序 */
    private static final String[][] DEFAULT_EXPENSE = {
            {"餐饮", "food", "1"},
            {"交通", "traffic", "2"},
            {"购物", "shopping", "3"},
            {"车票", "ticket", "4"},
            {"居住", "home", "5"},
            {"通讯", "communication", "6"},
            {"娱乐", "entertainment", "7"},
            {"医疗", "medical", "8"},
            {"教育", "education", "9"},
            {"服饰", "clothing", "10"},
            {"人情", "social", "11"},
            {"其他支出", "other_expense", "12"}
    };

    /** 默认收入分类 */
    private static final String[][] DEFAULT_INCOME = {
            {"工资", "salary", "1"},
            {"奖金", "bonus", "2"},
            {"兼职", "parttime", "3"},
            {"理财", "finance", "4"},
            {"红包", "redpacket", "5"},
            {"其他收入", "other_income", "6"}
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultCategories(Long userId) {
        if (categoryDao.countByUser(userId) > 0) {
            return;
        }
        for (String[] item : DEFAULT_EXPENSE) {
            insertCategory(userId, item[0], 1, item[1], Integer.parseInt(item[2]));
        }
        for (String[] item : DEFAULT_INCOME) {
            insertCategory(userId, item[0], 2, item[1], Integer.parseInt(item[2]));
        }
    }

    private void insertCategory(Long userId, String name, int type, String icon, int sort) {
        LedgerCategory c = new LedgerCategory();
        c.setUserId(userId);
        c.setName(name);
        c.setType(type);
        c.setIcon(icon);
        c.setSort(sort);
        categoryDao.insert(c);
    }

    @Override
    public Long createCategory(Long userId, LedgerCategory category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "分类名不能为空");
        }
        if (category.getType() == null || (category.getType() != 1 && category.getType() != 2)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择分类类型（支出/收入）");
        }
        String name = category.getName().trim();
        if (categoryDao.selectByName(userId, name, category.getType()) != null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "同类型下分类名已存在");
        }
        category.setUserId(userId);
        category.setName(name);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        categoryDao.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(Long userId, LedgerCategory category) {
        if (category.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "缺少分类 ID");
        }
        if (categoryDao.selectById(category.getId(), userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!StringUtils.hasText(category.getName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "分类名不能为空");
        }
        category.setUserId(userId);
        categoryDao.update(category);
    }

    @Override
    public void deleteCategory(Long userId, Long id) {
        if (categoryDao.selectById(id, userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 分类删除保护：分类下仍有流水时不允许删除
        if (categoryDao.countRecords(userId, id) > 0) {
            throw new BizException(ResultCode.CATEGORY_IN_USE);
        }
        categoryDao.logicDelete(id, userId);
    }

    /* ================= 按来源入账 ================= */

    @Override
    public Long postBySource(Long userId, Long categoryId, BigDecimal amount, LocalDateTime time,
                             String remark, Integer sourceType, Long sourceId) {
        // 幂等：同一来源（同一凭证 / 同一月份工资）只入账一次
        LedgerRecord exists = recordDao.selectBySource(userId, sourceType, sourceId);
        if (exists != null) {
            return exists.getId();
        }
        if (categoryId == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择记账分类");
        }
        LedgerRecord record = new LedgerRecord();
        record.setUserId(userId);
        record.setCategoryId(categoryId);
        record.setAmount(amount);
        record.setRecordTime(time == null ? LocalDateTime.now() : time);
        record.setRemark(remark);
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        validateRecord(userId, record);
        recordDao.insert(record);
        return record.getId();
    }

    /* ================= 私有方法 ================= */

    /** 校验：金额 > 0、分类存在且属于本人 */
    private void validateRecord(Long userId, LedgerRecord record) {
        if (record.getAmount() == null || record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "金额必须大于 0");
        }
        if (record.getCategoryId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择分类");
        }
        if (categoryDao.selectById(record.getCategoryId(), userId) == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "分类不存在或不属于当前用户");
        }
    }

    private LocalDateTime parseStart(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        return LocalDate.parse(date.trim()).atStartOfDay();
    }

    /** endDate 含当天，故取次日 0 点作为排他上界 */
    private LocalDateTime parseEnd(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        return LocalDate.parse(date.trim()).plusDays(1).atStartOfDay();
    }
}
