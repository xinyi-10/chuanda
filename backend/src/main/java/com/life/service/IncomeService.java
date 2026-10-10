package com.life.service;

import com.life.domain.IncomeConfig;
import com.life.domain.vo.IncomeConfigVO;

/** 固定收入配置服务 */
public interface IncomeService {

    /** 读取配置（含剩余修改次数、预期到账区间；未填写时 initialized=false） */
    IncomeConfigVO getConfig(Long userId);

    /** 首次填写（初始化）——不计入修改次数、不限次数 */
    IncomeConfigVO init(Long userId, IncomeConfig config);

    /** 修改配置——受「自然月最多 2 次」限制 */
    IncomeConfigVO update(Long userId, IncomeConfig config);
}
