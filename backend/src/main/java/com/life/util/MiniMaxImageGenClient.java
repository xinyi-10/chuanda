package com.life.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * MiniMax image-01 生图实现。
 *
 * <p>接口：{@code POST https://api.minimaxi.com/v1/image_generation}。
 * 文生图：请求体 {@code {model, prompt, aspect_ratio, response_format}}；
 * 图生图（角色一致性）：额外传 {@code subject_reference: [{type:"character", image_file:...}]}，
 * {@code image_file} 用 Base64 Data URL（本系统图存 RustFS，非公网 URL，故用 base64）。
 * 任何异常统一向上抛，由异步生成器 catch 并置失败。</p>
 */
@Component
public class MiniMaxImageGenClient implements ImageGenClient {

    private static final Logger log = LoggerFactory.getLogger(MiniMaxImageGenClient.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${minimax.api-key:}")
    private String apiKey;

    @Value("${minimax.base-url:https://api.minimaxi.com}")
    private String baseUrl;

    @Value("${minimax.image-model:image-01}")
    private String model;

    @Override
    public byte[] textToImage(String prompt, String aspectRatio) {
        return doGenerate(prompt, aspectRatio, null);
    }

    @Override
    public byte[] textToImageWithRef(String prompt, String aspectRatio, byte[] refImage) {
        if (refImage == null || refImage.length == 0) {
            throw new IllegalArgumentException("参考图不能为空");
        }
        ObjectNode ref = mapper.createObjectNode();
        ref.put("type", "character");
        ref.put("image_file", "data:image/png;base64," + Base64.getEncoder().encodeToString(refImage));
        return doGenerate(prompt, aspectRatio, ref);
    }

    private byte[] doGenerate(String prompt, String aspectRatio, ObjectNode subjectReference) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("未配置 MiniMax API Key（app.properties 的 minimax.api-key）");
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("aspect_ratio", aspectRatio == null || aspectRatio.isEmpty() ? "16:9" : aspectRatio);
            body.put("response_format", "url");
            if (subjectReference != null) {
                ArrayNode arr = mapper.createArrayNode();
                arr.add(subjectReference);
                body.set("subject_reference", arr);
            }

            String resp = httpPost(baseUrl + "/v1/image_generation", body.toString());
            JsonNode root = mapper.readTree(resp);
            JsonNode urls = root.path("data").path("image_urls");
            if (urls.isArray() && urls.size() > 0) {
                return download(urls.get(0).asText());
            }
            String err = root.path("base_resp").path("status_msg").asText("");
            throw new IllegalStateException("MiniMax 生图返回异常：" + (err.isEmpty() ? resp : err));
        } catch (Exception e) {
            log.warn("[imagegen] 生图失败：{}", e.getMessage());
            throw new RuntimeException("生图失败：" + e.getMessage(), e);
        }
    }

    private String httpPost(String url, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(120000);
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

    private byte[] download(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(120000);
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] tmp = new byte[8192];
            int n;
            while ((n = in.read(tmp)) != -1) {
                buf.write(tmp, 0, n);
            }
            return buf.toByteArray();
        }
    }
}
