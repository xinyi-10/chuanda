package com.life.service.impl;

import com.life.common.BizException;
import com.life.common.PageResult;
import com.life.common.ResultCode;
import com.life.dao.ReceiptCategoryDao;
import com.life.dao.ReceiptImageDao;
import com.life.domain.ReceiptCategory;
import com.life.domain.ReceiptImage;
import com.life.domain.vo.OcrResult;
import com.life.domain.vo.UploadResultVO;
import com.life.service.LedgerService;
import com.life.service.ReceiptService;
import com.life.storage.FileStorage;
import com.life.util.ImageHashUtil;
import com.life.util.OcrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** 支付凭证服务实现 */
@Service
public class ReceiptServiceImpl implements ReceiptService {

    private static final List<String> ALLOWED_EXT = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @Value("${app.upload.phash-threshold:5}")
    private int phashThreshold;

    @Autowired
    private ReceiptImageDao receiptImageDao;

    @Autowired
    private ReceiptCategoryDao receiptCategoryDao;

    @Autowired
    private OcrClient ocrClient;

    /** 文件存储（本地磁盘 / RustFS，由 storage.type 配置切换） */
    @Autowired
    private FileStorage fileStorage;

    /** 记账服务：凭证确认后自动入账 */
    @Autowired
    private LedgerService ledgerService;

    @Override
    public List<ReceiptCategory> listCategories() {
        return receiptCategoryDao.listEnabled();
    }

    @Override
    public UploadResultVO upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择要上传的图片");
        }
        String originalName = file.getOriginalFilename();
        String ext = extOf(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(),
                    "仅支持图片格式：" + String.join("/", ALLOWED_EXT));
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "读取上传文件失败：" + e.getMessage());
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String key = "receipt/" + userId + "/" + storedName;

        try {
            String md5 = ImageHashUtil.md5(content);
            String phash = ImageHashUtil.aHash(content);

            // ① 精确判重：MD5 命中 → 拒绝（此时尚未写入存储）
            ReceiptImage exists = receiptImageDao.selectByMd5(userId, md5);
            if (exists != null) {
                UploadResultVO dup = new UploadResultVO();
                dup.setDuplicate(true);
                dup.setSimilar(false);
                dup.setId(exists.getId());
                dup.setMessage("该图片已上传过（" + exists.getFileName() + "），已阻止重复上传");
                return dup;
            }

            // ② 近似判重：感知哈希汉明距离 <= 阈值 → 提示但不拦截
            boolean similar = false;
            for (ReceiptImage fp : receiptImageDao.listFingerprints(userId)) {
                if (ImageHashUtil.hammingDistance(phash, fp.getPhash()) <= phashThreshold) {
                    similar = true;
                    break;
                }
            }

            // ③ 写入对象存储（本地磁盘 / RustFS，由 FileStorage 决定）
            fileStorage.save(key, content, file.getContentType());

            // ④ 识别（审核 + 金额 + 时间）
            OcrResult ocr = ocrClient.recognize(content, originalName);

            // ④ 审核：必须同时具备「支付成功标识」与「明确金额」才自动通过
            boolean autoPass = ocr.isPaySuccess()
                    && ocr.getAmount() != null
                    && ocr.getAmount().compareTo(BigDecimal.ZERO) > 0;

            ReceiptImage image = new ReceiptImage();
            image.setUserId(userId);
            image.setFileName(originalName);
            image.setFilePath(key);
            image.setFileSize(file.getSize());
            image.setMd5(md5);
            image.setPhash(phash);
            image.setPayAmount(ocr.getAmount());
            image.setPayTime(ocr.getPayTime());
            image.setTimeSource(ocr.getPayTime() != null ? 1 : null);
            image.setAuditStatus(autoPass ? 1 : 0);
            image.setAuditMsg(buildAuditMsg(ocr, autoPass));
            image.setCategoryId(resolveCategoryId(ocr.getRawText()));

            receiptImageDao.insert(image);

            UploadResultVO vo = new UploadResultVO();
            vo.setId(image.getId());
            vo.setDuplicate(false);
            vo.setSimilar(similar);
            vo.setAuditStatus(image.getAuditStatus());
            vo.setAuditMsg(image.getAuditMsg());
            vo.setCategoryId(image.getCategoryId());
            ReceiptCategory category = image.getCategoryId() == null ? null : receiptCategoryDao.selectById(image.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());
            vo.setPayAmount(image.getPayAmount());
            vo.setPayTime(image.getPayTime());
            vo.setNeedTime(image.getPayTime() == null);
            vo.setMessage(buildMessage(similar, autoPass));
            return vo;
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "凭证处理失败：" + e.getMessage());
        }
    }

    @Override
    public PageResult<ReceiptImage> page(Long userId, Long categoryId, Integer auditStatus,
                                         Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 100) ? 12 : pageSize;

        long total = receiptImageDao.countByCondition(userId, categoryId, auditStatus);
        List<ReceiptImage> list = receiptImageDao.listByPage(userId, categoryId, auditStatus,
                (pn - 1) * ps, ps);
        for (ReceiptImage img : list) {
            img.setNeedTime(img.getPayTime() == null);
            img.setUrl("/receipt/file/" + img.getId());
        }
        return PageResult.of(list, total, pn, ps);
    }

    @Override
    public ReceiptImage getById(Long id, Long userId) {
        ReceiptImage image = receiptImageDao.selectById(id, userId);
        if (image == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        image.setNeedTime(image.getPayTime() == null);
        image.setUrl("/receipt/file/" + image.getId());
        return image;
    }

    @Override
    public ReceiptImage fillTime(Long userId, Long id, BigDecimal payAmount,
                                 LocalDateTime payTime, String remark, Long ledgerCategoryId) {
        ReceiptImage image = receiptImageDao.selectById(id, userId);
        if (image == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请填写正确的支付金额");
        }
        if (payTime == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请填写支付时间");
        }
        image.setPayAmount(payAmount);
        image.setPayTime(payTime);
        image.setTimeSource(2);
        image.setRemark(remark);
        receiptImageDao.updateTimeAndAmount(image);

        // 自动入账：写入记账流水（幂等，sourceType=2、sourceId=凭证ID；收支方向由分类 type 派生）
        ledgerService.postBySource(userId, ledgerCategoryId, payAmount, payTime,
                "凭证入账：" + (image.getFileName() == null ? "" : image.getFileName()),
                2, id);

        return getById(id, userId);
    }

    @Override
    public ReceiptImage correctCategory(Long userId, Long id, Long categoryId) {
        if (receiptImageDao.selectById(id, userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (categoryId != null && receiptCategoryDao.selectById(categoryId) == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "分类不存在");
        }
        receiptImageDao.updateCategory(id, userId, categoryId);
        return getById(id, userId);
    }

    @Override
    public void delete(Long userId, Long id) {
        receiptImageDao.logicDelete(id, userId);
    }

    /* ================= 私有方法 ================= */

    private String extOf(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 按分类关键词匹配识别文本，命中则归类，否则归入 other */
    private Long resolveCategoryId(String rawText) {
        List<ReceiptCategory> categories = receiptCategoryDao.listEnabled();
        String text = rawText == null ? "" : rawText;
        for (ReceiptCategory c : categories) {
            if ("other".equalsIgnoreCase(c.getCode()) || !StringUtils.hasText(c.getKeywords())) {
                continue;
            }
            for (String kw : c.getKeywords().split(",")) {
                String k = kw.trim();
                if (!k.isEmpty() && text.contains(k)) {
                    return c.getId();
                }
            }
        }
        for (ReceiptCategory c : categories) {
            if ("other".equalsIgnoreCase(c.getCode())) {
                return c.getId();
            }
        }
        return null;
    }

    private String buildAuditMsg(OcrResult ocr, boolean autoPass) {
        if (autoPass) {
            return "自动审核通过：已识别到支付成功标识与金额";
        }
        if (!ocr.isSuccess()) {
            return "自动识别未成功，请手动补充支付金额与时间";
        }
        boolean hasFlag = ocr.isPaySuccess();
        boolean hasAmount = ocr.getAmount() != null && ocr.getAmount().compareTo(BigDecimal.ZERO) > 0;
        if (!hasFlag && !hasAmount) {
            return "未识别到「支付成功」标识与支付金额，请手动补充并确认";
        }
        if (!hasAmount) {
            return "已识别到支付成功标识，但未识别到金额，请补充金额";
        }
        return "已识别到金额，但未检测到「支付成功」标识，请确认后补充";
    }

    private String buildMessage(boolean similar, boolean autoPass) {
        if (similar) {
            return "检测到与已有凭证高度相似，请确认是否重复上传";
        }
        return autoPass ? "上传成功，已自动审核并归类" : "上传成功，需人工确认金额与时间";
    }
}
