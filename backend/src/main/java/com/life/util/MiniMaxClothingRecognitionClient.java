package com.life.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.life.domain.vo.ClothingRecognitionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * MiniMax 视觉模型（MiniMax-VL-01）衣物识别实现。
 *
 * <p>接口：{@code POST https://api.minimaxi.com/v1/coding_plan/vlm}，
 * 请求体 {@code {prompt, image_url}}（image_url 用 Base64 Data URL）。
 * 模型返回文本内容，后端提取其中的 JSON 再映射成标准字典编码。
 * 任何异常（key 缺失 / 网络 / 解析失败）都返回 {@code recognized=false}，
 * 由前端兜底让用户手动填写。</p>
 */
@Component
public class MiniMaxClothingRecognitionClient implements ClothingRecognitionClient {

    private static final Logger log = LoggerFactory.getLogger(MiniMaxClothingRecognitionClient.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${minimax.api-key:}")
    private String apiKey;

    @Value("${minimax.base-url:https://api.minimaxi.com}")
    private String baseUrl;

    @Override
    public ClothingRecognitionResult recognize(byte[] image) {
        ClothingRecognitionResult result = new ClothingRecognitionResult();
        result.setRecognized(false);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return result;
        }
        try {
            String prompt = "识别这张衣物图片，只返回一个 JSON 对象（不要任何其他文字），格式："
                    + "{\"category\":\"top\",\"season\":\"all\",\"color\":\"白色\",\"name\":\"白色T恤\"}。"
                    + "category 必须从 top(上装)/bottom(下装)/outer(外套)/shoes(鞋)/accessory(配饰) 中选一个；"
                    + "season 必须从 spring(春)/summer(夏)/autumn(秋)/winter(冬)/all(四季通用) 中选一个；"
                    + "color 用简洁的中文颜色名；name 用「颜色+品类」如「白色T恤」。";

            String body = "{\"prompt\":" + mapper.writeValueAsString(prompt)
                    + ",\"image_url\":\"data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(image) + "\"}";

            String resp = httpPost(baseUrl + "/v1/coding_plan/vlm", body);
            JsonNode root = mapper.readTree(resp);
            if (root.path("base_resp").path("status_code").asInt(-1) != 0) {
                log.warn("[clothing-rec] VL 返回异常：{}", resp);
                return result;
            }
            String content = root.path("content").asText("");
            if (content.isEmpty()) {
                return result;
            }
            return parseContent(content);
        } catch (Exception e) {
            log.warn("[clothing-rec] 识别失败：{}", e.getMessage());
            return result;
        }
    }

    /** 从模型输出中提取 JSON 并映射成标准编码 */
    private ClothingRecognitionResult parseContent(String content) {
        ClothingRecognitionResult result = new ClothingRecognitionResult();
        result.setRecognized(false);
        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return result;
            }
            JsonNode node = mapper.readTree(content.substring(start, end + 1));
            String category = mapCategory(node.path("category").asText(""));
            String season = mapSeason(node.path("season").asText(""));
            String color = node.path("color").asText("").trim();
            String name = node.path("name").asText("").trim();

            if (category == null || season == null) {
                return result;
            }
            result.setCategory(category);
            result.setSeason(season);
            result.setColor(color);
            result.setName(name);
            result.setRecognized(true);
        } catch (Exception ignored) {
            // 解析失败，保持 recognized=false
        }
        return result;
    }

    /** 类别中文/英文 → 标准编码 */
    private String mapCategory(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase();
        if (s.contains("top") || raw.contains("上装") || raw.contains("上衣") || raw.contains("T恤")
                || raw.contains("衬衫") || raw.contains("毛衣") || raw.contains("卫衣") || raw.contains("打底")) {
            return "top";
        }
        if (s.contains("bottom") || raw.contains("下装") || raw.contains("裤") || raw.contains("裙")
                || raw.contains("牛仔裤") || raw.contains("短裤")) {
            return "bottom";
        }
        if (s.contains("outer") || raw.contains("外套") || raw.contains("夹克") || raw.contains("大衣")
                || raw.contains("羽绒") || raw.contains("风衣") || raw.contains("棉服")) {
            return "outer";
        }
        if (s.contains("shoes") || raw.contains("鞋") || raw.contains("靴") || raw.contains("凉鞋")) {
            return "shoes";
        }
        if (s.contains("accessory") || raw.contains("配饰") || raw.contains("包") || raw.contains("帽")
                || raw.contains("围巾") || raw.contains("首饰") || raw.contains("眼镜")) {
            return "accessory";
        }
        return null;
    }

    /** 季节中文/英文 → 标准编码 */
    private String mapSeason(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase();
        if (s.contains("spring") || raw.contains("春")) {
            return "spring";
        }
        if (s.contains("summer") || raw.contains("夏")) {
            return "summer";
        }
        if (s.contains("autumn") || s.contains("fall") || raw.contains("秋")) {
            return "autumn";
        }
        if (s.contains("winter") || raw.contains("冬")) {
            return "winter";
        }
        if (s.contains("all") || raw.contains("四季") || raw.contains("通用") || raw.contains("全年")) {
            return "all";
        }
        return null;
    }

    private String httpPost(String url, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        try (java.io.OutputStream out = conn.getOutputStream()) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        try (InputStream is = in; ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] tmp = new byte[4096];
            int n;
            while ((n = is.read(tmp)) != -1) {
                buf.write(tmp, 0, n);
            }
            return buf.toString(StandardCharsets.UTF_8.name());
        }
    }
}
