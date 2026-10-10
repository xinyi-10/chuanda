package com.life.service.impl;

import com.life.dao.LedgerStatDao;
import com.life.domain.vo.CategoryStatVO;
import com.life.domain.vo.StatOverviewVO;
import com.life.domain.vo.TrendStatVO;
import com.life.service.LedgerStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** 记账统计服务实现 */
@Service
public class LedgerStatServiceImpl implements LedgerStatService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    private LedgerStatDao statDao;

    @Override
    public StatOverviewVO overview(Long userId, String month) {
        YearMonth ym = StringUtils.hasText(month) ? YearMonth.parse(month.trim()) : YearMonth.now();
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        Map<String, Object> sum = statDao.sumRange(userId, start, end);
        BigDecimal income = toDecimal(sum.get("totalIncome"));
        BigDecimal expense = toDecimal(sum.get("totalExpense"));

        // 分类占比：支出项按总支出算、收入项按总收入算
        List<CategoryStatVO> byCategory = statDao.statByCategory(userId, start, end);
        for (CategoryStatVO c : byCategory) {
            BigDecimal base = (c.getType() != null && c.getType() == 2) ? income : expense;
            if (base.compareTo(BigDecimal.ZERO) > 0 && c.getAmount() != null) {
                c.setPercent(c.getAmount().multiply(HUNDRED).divide(base, 1, RoundingMode.HALF_UP));
            } else {
                c.setPercent(BigDecimal.ZERO);
            }
        }

        StatOverviewVO vo = new StatOverviewVO();
        vo.setMonth(ym.toString());
        vo.setTotalIncome(income);
        vo.setTotalExpense(expense);
        vo.setBalance(income.subtract(expense));
        vo.setByCategory(byCategory);
        return vo;
    }

    @Override
    public List<TrendStatVO> trend(Long userId, Integer months) {
        int n = (months == null || months < 1 || months > 24) ? 6 : months;
        LocalDateTime start = YearMonth.now().minusMonths(n - 1L).atDay(1).atStartOfDay();

        List<TrendStatVO> list = statDao.statTrend(userId, start);
        for (TrendStatVO t : list) {
            BigDecimal income = t.getIncome() == null ? BigDecimal.ZERO : t.getIncome();
            BigDecimal expense = t.getExpense() == null ? BigDecimal.ZERO : t.getExpense();
            t.setIncome(income);
            t.setExpense(expense);
            t.setBalance(income.subtract(expense));
        }
        return list;
    }

    /** Map 中的数值可能是 BigDecimal / Long / null，统一转 BigDecimal */
    private BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        if (v instanceof Number) {
            return new BigDecimal(v.toString());
        }
        return new BigDecimal(v.toString());
    }
}
