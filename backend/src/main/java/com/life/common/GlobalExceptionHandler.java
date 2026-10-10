package com.life.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器（P0 基建）。
 *
 * <p>统一把异常转换为 {@link Result} JSON，避免异常堆栈直接暴露给前端。
 * 业务异常只回 code+msg；系统异常回 500 并记录完整堆栈。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：可预期，不打印堆栈 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验异常（@Valid / 必填参数缺失） */
    @ExceptionHandler({MethodArgumentNotValidException.class, MissingServletRequestParameterException.class})
    public Result<Void> handleParam(Exception e) {
        String msg = e instanceof MethodArgumentNotValidException
                ? ((MethodArgumentNotValidException) e).getBindingResult().getFieldError() != null
                    ? ((MethodArgumentNotValidException) e).getBindingResult().getFieldError().getDefaultMessage()
                    : ResultCode.PARAM_ERROR.getMsg()
                : e.getMessage();
        log.warn("参数异常: {}", msg);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 兜底：未预期异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.FAIL.getCode(), "服务开小差了，请稍后重试");
    }
}
