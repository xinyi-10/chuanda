package com.life.util;

import com.life.domain.vo.WeatherInfo;

/**
 * 天气客户端（可插拔）。
 *
 * <p>默认实现 {@link WttrInWeatherClient} 走免费公开接口 wttr.in（免 key），
 * 调用失败时自动降级为「按月份的季节策略」，保证推荐链路永不中断 ——
 * 与「LLM 失败走规则兜底」同一套路。</p>
 */
public interface WeatherClient {

    /**
     * 查询城市天气。
     *
     * @param city 城市名（中文或拼音均可，如「武汉」「Wuhan」）
     * @return 天气信息；{@code fromApi=false} 表示已降级
     */
    WeatherInfo getWeather(String city);
}
