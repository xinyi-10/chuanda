package com.life.common;

/**
 * 业务状态码字典（P0 基建）。
 *
 * <p>约定：200 成功；4xx/5xx 复用 HTTP 语义码（与全局异常对应）；
 * 1xxx 为业务码段，前端可按码做差异化提示，避免前后端各写各的。</p>
 */
public enum ResultCode {

    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    FAIL(500, "操作失败"),

    /* ===== 业务码段 1xxx ===== */
    USERNAME_EXISTS(1001, "用户名已存在"),
    LOGIN_FAILED(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),
    CATEGORY_IN_USE(1004, "该分类下仍有数据，无法删除"),
    TASK_NOT_FOUND(1005, "任务不存在或已过期"),
    EXTERNAL_API_ERROR(1006, "外部服务暂不可用，已使用兜底策略");

    private final Integer code;
    private final String msg;

    ResultCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
