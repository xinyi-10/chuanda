package com.life.controller;

import com.life.common.PageResult;
import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.OutfitImage;
import com.life.service.OutfitImageService;
import com.life.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/** 穿搭效果图（AI 生图）接口 */
@RestController
@RequestMapping("/outfit/image")
public class OutfitImageController {

    @Autowired
    private OutfitImageService outfitImageService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 触发生成：POST /outfit/image?outfitRecordId=&occasion=，返回任务 id */
    @PostMapping
    public Result<Long> generate(@RequestParam(required = false) Long outfitRecordId,
                                 @RequestParam(required = false) String occasion) {
        return Result.ok("已提交生成", outfitImageService.generate(UserContext.getUserId(), outfitRecordId, occasion));
    }

    /** 查询状态：GET /outfit/image/status/{id} */
    @GetMapping("/status/{id}")
    public Result<OutfitImage> status(@PathVariable Long id) {
        return Result.ok(outfitImageService.status(UserContext.getUserId(), id));
    }

    /** 历史列表：GET /outfit/image/list */
    @GetMapping("/list")
    public Result<PageResult<OutfitImage>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(outfitImageService.history(UserContext.getUserId(), pageNum, pageSize));
    }

    /** 设为我的形象：POST /outfit/image/avatar?imageId=xxx（之后生图走图生图锁定人物） */
    @PostMapping("/avatar")
    public Result<Void> setAvatar(@RequestParam Long imageId) {
        outfitImageService.setAvatar(UserContext.getUserId(), imageId);
        return Result.ok("已设为我的形象", null);
    }

    /**
     * 读取效果图：GET /outfit/image/file/{id}?token=xxx
     * 该路径在拦截器中放行（img 标签带不了 Authorization 头），用 token 参数手动鉴权。
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
        byte[] data = outfitImageService.readImage(userId, id);
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
}
