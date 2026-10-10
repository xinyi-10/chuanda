package com.life.service.impl;

import com.life.common.BizException;
import com.life.common.ResultCode;
import com.life.dao.IncomeConfigDao;
import com.life.domain.IncomeConfig;
import com.life.domain.vo.IncomeConfigVO;
import com.life.service.IncomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

/** 固定收入配置服务实现 */
@Service
public class IncomeServiceImpl implements IncomeService {

    /** 每月修改次数上限（自然月重置） */
    private static final int MAX_MODIFY_PER_MONTH = 2;

    @Autowired
    private IncomeConfigDao incomeConfigDao;

    @Override
    public IncomeConfigVO getConfig(Long userId) {
        IncomeConfig config = incomeConfigDao.selectByUser(userId);
        if (config == null) {
            IncomeConfigVO vo = new IncomeConfigVO();
            vo.setInitialized(false);
            vo.setModifyCount(0);
            vo.setRemainModify(MAX_MODIFY_PER_MONTH);
            vo.setCurrentMonth(currentMonth());
            return vo;
        }
        return toVO(config);
    }

    @Override
    public IncomeConfigVO init(Long userId, IncomeConfig config) {
        validate(config);
        if (incomeConfigDao.selectByUser(userId) != null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "已初始化，请使用「修改」功能");
        }
        // 首次填写：不计入修改次数
        config.setUserId(userId);
        config.setModifyCount(0);
        config.setLastModifyMonth(currentMonth());
        incomeConfigDao.insert(config);
        return getConfig(userId);
    }

    @Override
    public IncomeConfigVO update(Long userId, IncomeConfig config) {
        validate(config);
        IncomeConfig exist = incomeConfigDao.selectByUser(userId);
        if (exist == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请先完成首次填写");
        }

        String month = currentMonth();
        int used = exist.getModifyCount() == null ? 0 : exist.getModifyCount();

        // 自然月重置：进入新月份则计数清零
        if (!month.equals(exist.getLastModifyMonth())) {
            used = 0;
            incomeConfigDao.resetModifyCount(exist.getId(), month);
        }
        if (used >= MAX_MODIFY_PER_MONTH) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(),
                    "本月修改次数已用完（每月上限 " + MAX_MODIFY_PER_MONTH + " 次，次月 1 日自动重置）");
        }

        config.setId(exist.getId());
        config.setUserId(userId);
        config.setModifyCount(used + 1);
        config.setLastModifyMonth(month);
        incomeConfigDao.update(config);
        return getConfig(userId);
    }

    /* ================= 私有方法 ================= */

    private void validate(IncomeConfig config) {
        if (config.getMonthlyAmount() == null || config.getMonthlyAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "每月收入金额必须大于 0");
        }
        if (config.getPayDay() == null || config.getPayDay() < 1 || config.getPayDay() > 31) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "发薪日需在 1~31 之间");
        }
        if (config.getDelayDays() == null) {
            config.setDelayDays(0);
        }
        if (config.getDelayDays() < 0 || config.getDelayDays() > 31) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "延迟天数需在 0~31 之间");
        }
    }

    private IncomeConfigVO toVO(IncomeConfig config) {
        String month = currentMonth();
        int used = config.getModifyCount() == null ? 0 : config.getModifyCount();
        // 展示时也应用自然月规则：跨月视为已用 0
        if (!month.equals(config.getLastModifyMonth())) {
            used = 0;
        }

        IncomeConfigVO vo = new IncomeConfigVO();
        vo.setId(config.getId());
        vo.setMonthlyAmount(config.getMonthlyAmount());
        vo.setPayDay(config.getPayDay());
        vo.setDelayDays(config.getDelayDays());
        vo.setModifyCount(used);
        vo.setRemainModify(Math.max(0, MAX_MODIFY_PER_MONTH - used));
        vo.setCurrentMonth(month);
        vo.setPayDateRange(buildRange(config.getPayDay(), config.getDelayDays()));
        vo.setInitialized(true);
        return vo;
    }

    /** 构造预期到账区间描述，处理跨月（如 30 日 + 5 天 → 次月 4 日） */
    private String buildRange(Integer payDay, Integer delayDays) {
        int start = payDay == null ? 1 : payDay;
        int end = start + (delayDays == null ? 0 : delayDays);
        String endText = end > 31 ? ("次月 " + (end - 31) + " 日") : (end + " 日");
        if (end == start) {
            return "每月 " + start + " 日";
        }
        return "每月 " + start + " 日 ~ " + endText;
    }

    private String currentMonth() {
        return YearMonth.now().toString();
    }
}
