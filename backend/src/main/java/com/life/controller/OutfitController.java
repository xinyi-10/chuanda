package com.life.controller;

import com.life.common.PageResult;
import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.BodyProfile;
import com.life.domain.ClothingItem;
import com.life.domain.OutfitRecord;
import com.life.domain.vo.OutfitSuggestVO;
import com.life.domain.vo.ClothingRecognitionResult;
import com.life.service.OutfitService;
import com.life.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;

/** 穿搭推荐接口 */
@RestController
@RequestMapping("/outfit")
public class OutfitController {

    @Autowired
    private OutfitService outfitService;

    @Autowired
    private JwtUtil jwtUtil;

    /* ===== 身体数据 ===== */

    /** 读取身体数据：GET /outfit/body（未填写返回 data=null） */
    @GetMapping("/body")
    public Result<BodyProfile> getBody() {
        return Result.ok(outfitService.getBody(UserContext.getUserId()));
    }

    /** 保存身体数据：POST /outfit/body */
    @PostMapping("/body")
    public Result<BodyProfile> saveBody(@RequestBody BodyProfile profile) {
        return Result.ok("已保存", outfitService.saveBody(UserContext.getUserId(), profile));
    }

    /* ===== 衣物库 ===== */

    /** 衣物列表：GET /outfit/clothing?category=&season=&style= */
    @GetMapping("/clothing")
    public Result<List<ClothingItem>> listClothing(@RequestParam(required = false) String category,
                                                   @RequestParam(required = false) String season,
                                                   @RequestParam(required = false) String style) {
        return Result.ok(outfitService.listClothing(UserContext.getUserId(), category, season, style));
    }

    /** 新增 / 编辑衣物：POST /outfit/clothing */
    @PostMapping("/clothing")
    public Result<Long> saveClothing(@RequestBody ClothingItem item) {
        return Result.ok("已保存", outfitService.saveClothing(UserContext.getUserId(), item));
    }

    /** 删除衣物：DELETE /outfit/clothing/{id} */
    @DeleteMapping("/clothing/{id}")
    public Result<Void> deleteClothing(@PathVariable Long id) {
        outfitService.deleteClothing(UserContext.getUserId(), id);
        return Result.ok("删除成功", null);
    }

    /** 上传衣物图片：POST /outfit/clothing/image（multipart，返回存储 key） */
    @PostMapping("/clothing/image")
    public Result<String> uploadClothingImage(@RequestParam("file") MultipartFile file) {
        return Result.ok("上传成功", outfitService.uploadClothingImage(UserContext.getUserId(), file));
    }

    /** 识别衣物图片：POST /outfit/clothing/recognize（multipart，返回类别/季节/颜色） */
    @PostMapping("/clothing/recognize")
    public Result<ClothingRecognitionResult> recognizeClothing(@RequestParam("file") MultipartFile file) {
        return Result.ok(outfitService.recognizeClothing(file));
    }

    /**
     * 读取衣物图片：GET /outfit/clothing/image/{id}?token=xxx
     * 该路径在拦截器中放行（img 标签带不了 Authorization 头），用 token 参数手动鉴权。
     */
    @GetMapping("/clothing/image/{id}")
    public void clothingImage(@PathVariable Long id,
                              @RequestParam(required = false) String token,
                              HttpServletResponse response) {
        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        byte[] data = outfitService.readClothingImage(userId, id);
        if (data == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType("image/png");
        try (OutputStream out = response.getOutputStream()) {
            out.write(data);
            out.flush();
        } catch (Exception ignored) {
            // 客户端可能已断开
        }
    }

    /* ===== 推荐 ===== */

    /** 生成推荐：POST /outfit/recommend?occasion=上班 */
    @PostMapping("/recommend")
    public Result<OutfitSuggestVO> recommend(@RequestParam(required = false) String occasion) {
        return Result.ok(outfitService.recommend(UserContext.getUserId(), occasion));
    }

    /** 推荐历史：GET /outfit/history */
    @GetMapping("/history")
    public Result<PageResult<OutfitRecord>> history(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(outfitService.history(UserContext.getUserId(), pageNum, pageSize));
    }
}
