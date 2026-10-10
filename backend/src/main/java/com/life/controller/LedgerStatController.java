package com.life.controller;

import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.vo.StatOverviewVO;
import com.life.domain.vo.TrendStatVO;
import com.life.service.LedgerStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 记账统计接口 */
@RestController
@RequestMapping("/ledger/stat")
public class LedgerStatController {

    @Autowired
    private LedgerStatService statService;

    /** 月度收支总览 + 分类占比：GET /ledger/stat/overview?month=2026-10 */
    @GetMapping("/overview")
    public Result<StatOverviewVO> overview(@RequestParam(required = false) String month) {
        return Result.ok(statService.overview(UserContext.getUserId(), month));
    }

    /** 近 N 个月趋势：GET /ledger/stat/trend?months=6 */
    @GetMapping("/trend")
    public Result<List<TrendStatVO>> trend(@RequestParam(required = false) Integer months) {
        return Result.ok(statService.trend(UserContext.getUserId(), months));
    }
}
