package com.life.service;

import com.life.dao.IncomeConfigDao;
import com.life.dao.LedgerCategoryDao;
import com.life.domain.IncomeConfig;
import com.life.domain.LedgerCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 工资自动入账调度器。
 *
 * <p><b>触发方式</b>：定时任务，每天 08:00 检查一次。</p>
 *
 * <p><b>处理流程</b>：</p>
 * <ol>
 *   <li>遍历所有已配置固定收入的用户；</li>
 *   <li>取发薪日 D（若超过当月天数则取当月最后一天，如 31 号遇到 2 月）；</li>
 *   <li>若今天 &lt; D → 未到发薪日，跳过；</li>
 *   <li>构造幂等键 {@code sourceId = YYYYMM}，交给 {@code LedgerService.postBySource}
 *       —— 它内部会先查 (sourceType=3, sourceId=YYYYMM) 是否已存在，已存在直接返回，<b>天然防重复</b>；</li>
 *   <li>取该用户的收入类分类（优先名为「工资」的，否则取第一个收入分类）；</li>
 *   <li>生成一条收入流水，记账时间 = 本月发薪日 00:00。</li>
 * </ol>
 *
 * <p><b>不实现接口</b>：避免 JDK 动态代理导致 @Scheduled 方法找不到（见 TodoScheduler 的说明）。</p>
 */
@Component
public class SalaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SalaryScheduler.class);

    /** 来源类型：工资自动入账 */
    private static final int SOURCE_SALARY = 3;

    @Autowired
    private IncomeConfigDao incomeConfigDao;

    @Autowired
    private LedgerCategoryDao ledgerCategoryDao;

    @Autowired
    private LedgerService ledgerService;

    /** 每天 08:00 检查 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void autoPostSalary() {
        postDueSalaries();
    }

    /** 实际入账逻辑（可被手动触发，便于测试与「立即补记」） */
    public int postDueSalaries() {
        YearMonth ym = YearMonth.now();
        LocalDate today = LocalDate.now();
        int posted = 0;
        try {
            List<IncomeConfig> configs = incomeConfigDao.listAll();
            for (IncomeConfig cfg : configs) {
                try {
                    if (cfg.getPayDay() == null || cfg.getMonthlyAmount() == null) {
                        continue;
                    }
                    // 发薪日可能超过当月天数（如 31 号遇到 2 月）→ 取当月最后一天
                    int payDay = Math.min(cfg.getPayDay(), ym.lengthOfMonth());
                    if (today.getDayOfMonth() < payDay) {
                        continue; // 未到发薪日
                    }

                    Long categoryId = resolveSalaryCategory(cfg.getUserId());
                    if (categoryId == null) {
                        log.warn("[salary] 用户 {} 没有收入类分类，跳过工资入账", cfg.getUserId());
                        continue;
                    }

                    long idempotentKey = ym.getYear() * 100L + ym.getMonthValue();
                    LocalDateTime payTime = ym.atDay(payDay).atStartOfDay();
                    ledgerService.postBySource(cfg.getUserId(), categoryId, cfg.getMonthlyAmount(),
                            payTime, "工资自动入账 " + ym, SOURCE_SALARY, idempotentKey);
                    posted++;
                    log.info("[salary] 用户 {} 已自动入账 {} 元工资（{}）",
                            cfg.getUserId(), cfg.getMonthlyAmount(), ym);
                } catch (Exception e) {
                    log.error("[salary] 用户 {} 工资入账失败", cfg.getUserId(), e);
                }
            }
        } catch (Exception e) {
            log.error("[salary] 工资入账扫描失败", e);
        }
        return posted;
    }

    /** 立即为指定用户补记当月工资（手动触发 / 测试用），返回是否产生入账 */
    public boolean postForUser(Long userId) {
        IncomeConfig cfg = incomeConfigDao.selectByUser(userId);
        if (cfg == null || cfg.getPayDay() == null || cfg.getMonthlyAmount() == null) {
            return false;
        }
        Long categoryId = resolveSalaryCategory(userId);
        if (categoryId == null) {
            return false;
        }
        YearMonth ym = YearMonth.now();
        int payDay = Math.min(cfg.getPayDay(), ym.lengthOfMonth());
        long idempotentKey = ym.getYear() * 100L + ym.getMonthValue();
        ledgerService.postBySource(userId, categoryId, cfg.getMonthlyAmount(),
                ym.atDay(payDay).atStartOfDay(), "工资自动入账 " + ym, SOURCE_SALARY, idempotentKey);
        return true;
    }

    /** 取收入类分类：优先「工资」，否则第一个收入分类 */
    private Long resolveSalaryCategory(Long userId) {
        List<LedgerCategory> incomeCategories = ledgerCategoryDao.listByUser(userId, 2);
        if (incomeCategories == null || incomeCategories.isEmpty()) {
            return null;
        }
        for (LedgerCategory c : incomeCategories) {
            if (c.getName() != null && c.getName().contains("工资")) {
                return c.getId();
            }
        }
        return incomeCategories.get(0).getId();
    }
}
