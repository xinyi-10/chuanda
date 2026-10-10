package com.life.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体：{ code, msg, data, timestamp }
 *
 * <p>与既有项目 zcgl_study / woye_gl 的字段约定保持一致，前端零改造成本。
 * timestamp 由后端生成（服务器时间戳），前端可据此做时区对齐。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码（见 {@link ResultCode}） */
    private Integer code;
    /** 提示信息 */
    private String msg;
    /** 业务数据 */
    private T data;
    /** 服务端时间戳（毫秒） */
    private Long timestamp;

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(ResultCode.FAIL.getCode(), msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> fail(ResultCode rc) {
        return new Result<>(rc.getCode(), rc.getMsg(), null);
    }
}
