package com.life.controller;

import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.SysUser;
import com.life.service.AuthService;
import com.life.storage.FileStorage;
import com.life.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权接口。
 *
 * <p>请求/响应均走统一格式 {@link Result}；登录与注册在拦截器中放行，
 * /auth/info 受保护（未登录返回 401）。</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String token = authService.register(body.get("username"), body.get("password"), body.get("nickname"));
        return Result.ok("注册成功", tokenPayload(token));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String token = authService.login(body.get("username"), body.get("password"));
        return Result.ok("登录成功", tokenPayload(token));
    }

    @GetMapping("/info")
    public Result<SysUser> info() {
        return Result.ok(authService.getById(UserContext.getUserId()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态：前端丢弃 token 即可；如要强制失效可后续接入 token 黑名单
        return Result.ok();
    }

    /** 上传头像：POST /auth/avatar（multipart） */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok("头像上传成功", authService.uploadAvatar(UserContext.getUserId(), file));
    }

    /**
     * 读取当前用户头像：GET /auth/avatar/file?token=xxx
     * 该路径在拦截器中放行（img 标签无法带 Authorization 头），用 token 参数手动鉴权。
     */
    @GetMapping("/avatar/file")
    public void avatarFile(@RequestParam(required = false) String token,
                           HttpServletResponse response) {
        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        SysUser user = authService.getById(userId);
        if (user == null || user.getAvatar() == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] data = fileStorage.read(user.getAvatar());
        if (data == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String key = user.getAvatar().toLowerCase();
        response.setContentType(key.endsWith(".png") ? "image/png"
                : key.endsWith(".gif") ? "image/gif"
                : key.endsWith(".webp") ? "image/webp" : "image/jpeg");
        try (OutputStream out = response.getOutputStream()) {
            out.write(data);
            out.flush();
        } catch (Exception ignored) {
            // 客户端可能已断开
        }
    }

    private Map<String, Object> tokenPayload(String token) {
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        return data;
    }
}
