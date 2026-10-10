package com.life.interceptor;

import com.life.common.UserContext;
import com.life.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录校验拦截器：校验请求头 {@code Authorization: Bearer <token>}。
 *
 * <p>校验通过后把 userId 写入 {@link UserContext}，供 Service 与数据隔离拦截器使用；
 * 请求结束时清理，防止线程复用串号。</p>
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // CORS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.trim().isEmpty()) {
            writeUnauthorized(response);
            return false;
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            UserContext.set(userId, jwtUtil.getUsername(token));
            return true;
        } catch (Exception e) {
            // 过期 / 篡改 / 格式错误 一律按未登录处理
            writeUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    /** 直接写统一响应体（不走 Controller，避免被 302 重定向） */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":401,\"msg\":\"未登录或登录已过期\",\"data\":null,\"timestamp\":"
                        + System.currentTimeMillis() + "}");
    }
}
