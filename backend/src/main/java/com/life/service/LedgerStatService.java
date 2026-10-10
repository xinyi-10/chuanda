package com.life.service;

import com.life.domain.vo.StatOverviewVO;
import com.life.domain.vo.TrendStatVO;

import java.util.List;

/** 记账统计服务 */
public interface LedgerStatService {

    /** 月度收支总览（含分类占比），month 形如 2026-10 */
    StatOverviewVO overview(Long userId, String month);

    /** 近 N 个月收支趋势 */
    List<TrendStatVO> trend(Long userId, Integer months);
}
