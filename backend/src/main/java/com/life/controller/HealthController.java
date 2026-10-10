package com.life.controller;

import com.life.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口（在拦截器中放行）。
 *
 * <p>用于阶段二验收：确认「工程能启动 + 能连库 + 统一响应格式正确」。</p>
 */
@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("app", "life-manager");
        data.put("status", "up");
        data.put("timestamp", System.currentTimeMillis());
        try (Connection conn = dataSource.getConnection()) {
            data.put("dbOk", true);
            data.put("database", conn.getCatalog());
        } catch (Exception e) {
            data.put("dbOk", false);
            data.put("dbError", e.getMessage());
        }
        return Result.ok(data);
    }
}
