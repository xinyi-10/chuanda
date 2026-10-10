package com.life.common;

/**
 * 当前登录用户上下文（基于 ThreadLocal）。
 *
 * <p>由 {@code LoginInterceptor} 在请求进入时写入、请求结束时清理；
 * 供 Service / MyBatis 数据隔离拦截器读取当前 userId，实现自动数据隔离。</p>
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, String username) {
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    /** 当前用户 ID；未登录返回 null */
    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    /** 必须在请求结束时调用，防止线程复用导致的用户串号 */
    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}
