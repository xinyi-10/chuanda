package com.life.controller;

import com.life.common.PageResult;
import com.life.common.Result;
import com.life.common.ResultCode;
import com.life.common.BizException;
import com.life.common.UserContext;
import com.life.domain.ReceiptCategory;
import com.life.domain.ReceiptImage;
import com.life.domain.vo.UploadResultVO;
import com.life.service.ReceiptService;
import com.life.storage.FileStorage;
import com.life.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 支付凭证接口（分类导览 / 上传 / 列表 / 时间补录 / 分类纠正 / 图片访问） */
@RestController
@RequestMapping("/receipt")
public class ReceiptController {

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 文件存储（本地磁盘 / RustFS，由配置切换） */
    @Autowired
    private FileStorage fileStorage;

    /** 分类导览：GET /receipt/category/list */
    @GetMapping("/category/list")
    public Result<List<ReceiptCategory>> categories() {
        return Result.ok(receiptService.listCategories());
    }

    /** 上传凭证：POST /receipt/upload（multipart） */
    @PostMapping("/upload")
    public Result<UploadResultVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(receiptService.upload(UserContext.getUserId(), file));
    }

    /** 分页列表：GET /receipt/page?categoryId=&auditStatus=&pageNum=&pageSize= */
    @GetMapping("/page")
    public Result<PageResult<ReceiptImage>> page(@RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) Integer auditStatus,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.ok(receiptService.page(UserContext.getUserId(), categoryId, auditStatus, pageNum, pageSize));
    }

    /** 详情：GET /receipt/{id} */
    @GetMapping("/{id}")
    public Result<ReceiptImage> detail(@PathVariable Long id) {
        return Result.ok(receiptService.getById(id, UserContext.getUserId()));
    }

    /** 补录 / 修正时间与金额，并自动入账：POST /receipt/{id}/time */
    @PostMapping("/{id}/time")
    public Result<ReceiptImage> fillTime(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("payAmount") == null ? null
                : new BigDecimal(body.get("payAmount").toString());
        LocalDateTime payTime = body.get("payTime") == null ? null
                : LocalDateTime.parse(body.get("payTime").toString());
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        Long ledgerCategoryId = body.get("ledgerCategoryId") == null ? null
                : Long.valueOf(body.get("ledgerCategoryId").toString());
        return Result.ok("已确认并入账", receiptService.fillTime(
                UserContext.getUserId(), id, amount, payTime, remark, ledgerCategoryId));
    }

    /** 纠正分类：PUT /receipt/{id}/category */
    @PutMapping("/{id}/category")
    public Result<ReceiptImage> correctCategory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long categoryId = body.get("categoryId") == null ? null
                : Long.valueOf(body.get("categoryId").toString());
        return Result.ok("已更新", receiptService.correctCategory(UserContext.getUserId(), id, categoryId));
    }

    /** 删除：DELETE /receipt/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        receiptService.delete(UserContext.getUserId(), id);
        return Result.ok("删除成功", null);
    }

    /**
     * 图片访问：GET /receipt/file/{id}?token=xxx
     *
     * <p>该路径在拦截器中放行（img 标签无法携带 Authorization 头），
     * 因此改为用 token 查询参数手动鉴权，并校验图片归属当前用户，避免越权访问。</p>
     */
    @GetMapping("/file/{id}")
    public void file(@PathVariable Long id,
                     @RequestParam(required = false) String token,
                     HttpServletResponse response) {
        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        ReceiptImage image = receiptService.getById(id, userId);

        byte[] data = fileStorage.read(image.getFilePath());
        if (data == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String name = image.getFileName() == null ? "" : image.getFileName().toLowerCase();
        response.setContentType(name.endsWith(".png") ? "image/png"
                : name.endsWith(".gif") ? "image/gif"
                : name.endsWith(".webp") ? "image/webp" : "image/jpeg");
        try (OutputStream out = response.getOutputStream()) {
            out.write(data);
            out.flush();
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "图片读取失败");
        }
    }
}
