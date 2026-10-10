package com.life.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.life.domain.vo.WeatherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * wttr.in 天气实现（免费、无需 API Key）。
 *
 * <p>接口：{@code https://wttr.in/{city}?format=j1}，返回 JSON。
 * 任何异常（网络不通 / 超时 / 城市名无法识别）都会降级为「按月份的季节策略」，
 * 保证前端永远拿得到一份可用天气，推荐流程不中断。</p>
 */
@Component
public class WttrInWeatherClient implements WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(WttrInWeatherClient.class);

    /** 各月大致气温（℃），用于降级策略 */
    private static final int[] MONTH_TEMP = {5, 7, 12, 18, 23, 27, 30, 29, 25, 20, 14, 8};

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public WeatherInfo getWeather(String city) {
        if (city == null || city.trim().isEmpty()) {
            return seasonFallback("未知");
        }
        String name = city.trim();
        try {
            String url = "https://wttr.in/" + URLEncoder.encode(name, "UTF-8") + "?format=j1";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "curl/7.68.0");
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                return seasonFallback(name);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            JsonNode root = mapper.readTree(sb.toString());
            JsonNode cur = root.path("current_condition").path(0);
            if (cur.isMissingNode()) {
                return seasonFallback(name);
            }

            WeatherInfo w = new WeatherInfo();
            w.setCity(name);
            w.setTemp(new BigDecimal(cur.path("temp_C").asText("0")));
            w.setFeelsLike(new BigDecimal(cur.path("FeelsLikeC").asText("0")));
            w.setCondition(cur.path("weatherDesc").path(0).path("value").asText("未知"));
            w.setHumidity(parseIntSafe(cur.path("humidity").asText("0")));
            w.setWind(cur.path("windspeedKmph").asText("0") + " km/h");
            w.setFromApi(true);
            w.setSeason(seasonByTemp(w.getTemp().intValue()));
            return w;
        } catch (Exception e) {
            log.warn("[weather] 获取「{}」天气失败，降级为季节策略：{}", name, e.getMessage());
            return seasonFallback(name);
        }
    }

    /** 降级：按当前月份给一个大致气温 */
    private WeatherInfo seasonFallback(String city) {
        int month = LocalDate.now().getMonthValue();
        int temp = MONTH_TEMP[month - 1];
        WeatherInfo w = new WeatherInfo();
        w.setCity(city);
        w.setTemp(BigDecimal.valueOf(temp));
        w.setFeelsLike(BigDecimal.valueOf(temp));
        w.setCondition("未知");
        w.setHumidity(null);
        w.setWind("-");
        w.setFromApi(false);
        w.setSeason(seasonByMonth(month));
        return w;
    }

    private String seasonByTemp(int temp) {
        if (temp >= 28) {
            return "夏";
        }
        if (temp >= 21) {
            return "春";
        }
        if (temp >= 11) {
            return "秋";
        }
        return "冬";
    }

    private String seasonByMonth(int month) {
        if (month >= 6 && month <= 8) {
            return "夏";
        }
        if (month >= 9 && month <= 11) {
            return "秋";
        }
        if (month == 12 || month <= 2) {
            return "冬";
        }
        return "春";
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
