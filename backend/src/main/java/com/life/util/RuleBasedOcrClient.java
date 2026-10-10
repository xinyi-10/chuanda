package com.life.util;

import com.life.domain.vo.OcrResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则版识别实现（默认兜底）。
 *
 * <p>⚠️ 说明：真正的「图片文字识别」需要 OCR 能力（云端 API 或本地引擎）。
 * 本项目在不接入外部服务的前提下，用<b>文件名 + 规则</b>模拟识别结果，
 * 目的是让「审核 / 归类 / 时间补录」整条业务链路可运行、可测试。</p>
 *
 * <p>接入真实 OCR 后，只需替换本类（或新增 @Primary 实现），
 * 把识别文本换成真实 OCR 文本即可，其余逻辑完全复用。</p>
 */
@Component
public class RuleBasedOcrClient implements OcrClient {

    /** 支付成功类标识词 */
    private static final String[] PAY_SUCCESS_WORDS = {
            "支付成功", "付款成功", "交易成功", "已支付", "已付款",
            "转账成功", "收款成功", "扣款成功", "支付完成"
    };

    /** 金额：形如 35.50 / 8000.00 */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d{1,9}\\.\\d{1,2})");

    /** 日期：2026-10-09 / 2026_10_09 / 20261009 / 2026年10月9日 */
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(20\\d{2})[-_/年]?(\\d{1,2})[-_/月]?(\\d{1,2})");

    @Override
    public OcrResult recognize(byte[] content, String fileName) {
        OcrResult result = new OcrResult();
        String text = fileName == null ? "" : fileName;
        result.setRawText(text);
        try {
            result.setSuccess(true);

            // ① 支付成功标识
            for (String word : PAY_SUCCESS_WORDS) {
                if (text.contains(word)) {
                    result.setPaySuccess(true);
                    break;
                }
            }

            // ② 金额
            Matcher am = AMOUNT_PATTERN.matcher(text);
            if (am.find()) {
                result.setAmount(new BigDecimal(am.group(1)));
            }

            // ③ 时间
            Matcher dm = DATE_PATTERN.matcher(text);
            if (dm.find()) {
                int y = Integer.parseInt(dm.group(1));
                int mo = Integer.parseInt(dm.group(2));
                int d = Integer.parseInt(dm.group(3));
                if (mo >= 1 && mo <= 12 && d >= 1 && d <= 31) {
                    result.setPayTime(LocalDateTime.of(y, mo, d, 0, 0));
                }
            }
        } catch (Exception e) {
            // 识别异常 → 降级为「未识别」，交由用户手动补充
            result.setSuccess(false);
        }
        return result;
    }
}
