package com.life.common;

/**
 * 业务异常：Service 层主动抛出，由 {@link GlobalExceptionHandler} 统一转成 {@link Result}。
 *
 * <p>与系统异常区分开：业务异常是「可预期的」，只返回 code + msg，不打印堆栈。</p>
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(String msg) {
        super(msg);
        this.code = ResultCode.FAIL.getCode();
    }

    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(ResultCode rc) {
        super(rc.getMsg());
        this.code = rc.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
