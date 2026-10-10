package com.life.service;

import com.life.common.PageResult;
import com.life.domain.BodyProfile;
import com.life.domain.ClothingItem;
import com.life.domain.OutfitRecord;
import com.life.domain.vo.OutfitSuggestVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 穿搭推荐服务 */
public interface OutfitService {

    /* ===== 身体数据 ===== */

    /** 读取身体数据（未填写返回 null），附带 BMI / 体型 */
    BodyProfile getBody(Long userId);

    /** 首次填写 / 更新身体数据 */
    BodyProfile saveBody(Long userId, BodyProfile profile);

    /* ===== 衣物库 ===== */

    List<ClothingItem> listClothing(Long userId, String category, String season, String style);

    Long saveClothing(Long userId, ClothingItem item);

    /**
     * 上传衣物图片（走统一文件存储 RustFS），返回存储 key。
     * 前端把该 key 写入 {@code ClothingItem.imageUrl} 后再保存衣物。
     */
    String uploadClothingImage(Long userId, MultipartFile file);

    /** 识别衣物图片（AI 视觉）：返回类别/季节/颜色/建议名称，识别失败 recognized=false */
    com.life.domain.vo.ClothingRecognitionResult recognizeClothing(MultipartFile file);

    /** 读取衣物图片（返回 key 对应的字节，供前端展示；不存在返回 null） */
    byte[] readClothingImage(Long userId, Long clothingId);

    void deleteClothing(Long userId, Long id);

    /* ===== 推荐 ===== */

    /** 生成穿搭推荐（天气 + 体型 + 场合 + 衣物库） */
    OutfitSuggestVO recommend(Long userId, String occasion);

    /** 推荐历史 */
    PageResult<OutfitRecord> history(Long userId, Integer pageNum, Integer pageSize);
}
