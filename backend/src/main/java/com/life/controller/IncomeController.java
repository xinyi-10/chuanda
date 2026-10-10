package com.life.controller;

import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.IncomeConfig;
import com.life.domain.vo.IncomeConfigVO;
import com.life.service.IncomeService;
import com.life.service.SalaryScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 固定收入（月工资）配置接口 */
@RestController
@RequestMapping("/income")
public class IncomeController {

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private SalaryScheduler salaryScheduler;

    /**
     * 立即补记当月工资：POST /income/post-now
     * 定时任务在发薪日自动入账；此接口用于手动补记（幂等，同月不会重复）。
     */
    @PostMapping("/post-now")
    public Result<Integer> postNow() {
        boolean ok = salaryScheduler.postForUser(UserContext.getUserId());
        return ok
                ? Result.ok("已按当月工资入账", 1)
                : Result.ok("未配置收入或缺少收入类分类，无法入账", 0);
    }

    /**
     * 读取配置：GET /income/config
     * 未填写时返回 initialized=false（前端据此弹首次引导），并带当月剩余修改次数。
     */
    @GetMapping("/config")
    public Result<IncomeConfigVO> config() {
        return Result.ok(incomeService.getConfig(UserContext.getUserId()));
    }

    /** 首次填写（初始化，不计入修改次数）：POST /income/init */
    @PostMapping("/init")
    public Result<IncomeConfigVO> init(@RequestBody IncomeConfig config) {
        return Result.ok("设置成功", incomeService.init(UserContext.getUserId(), config));
    }

    /** 修改配置（自然月最多 2 次）：PUT /income/config */
    @PutMapping("/config")
    public Result<IncomeConfigVO> update(@RequestBody IncomeConfig config) {
        return Result.ok("修改成功", incomeService.update(UserContext.getUserId(), config));
    }
}
