package com.life.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.life.common.BizException;
import com.life.common.PageResult;
import com.life.common.ResultCode;
import com.life.dao.BodyProfileDao;
import com.life.dao.OutfitImageDao;
import com.life.dao.OutfitRecordDao;
import com.life.domain.BodyProfile;
import com.life.domain.OutfitImage;
import com.life.domain.OutfitRecord;
import com.life.service.OutfitImageGenerator;
import com.life.service.OutfitImageService;
import com.life.storage.FileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 穿搭效果图服务实现 */
@Service
public class OutfitImageServiceImpl implements OutfitImageService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BodyProfileDao bodyProfileDao;

    @Autowired
    private OutfitRecordDao outfitRecordDao;

    @Autowired
    private OutfitImageDao outfitImageDao;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private OutfitImageGenerator generator;

    @Override
    public Long generate(Long userId, Long outfitRecordId, String occasion) {
        BodyProfile body = bodyProfileDao.selectByUser(userId);
        if (body == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请先填写身高体重三围");
        }
        OutfitRecord record = resolveRecord(userId, outfitRecordId);
        if (record == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请先生成一次穿搭推荐");
        }

        String prompt = buildPrompt(body, record);
        // 角色一致性：有「我的形象」参考图则读出，走图生图锁定人物
        byte[] refImage = loadAvatar(userId, body.getAvatarImageId());

        OutfitImage img = new OutfitImage();
        img.setUserId(userId);
        img.setOutfitRecordId(record.getId());
        img.setStatus(0);
        img.setPrompt(prompt);
        outfitImageDao.insert(img);

        // @Async 外部调用，异步执行生图，不阻塞请求线程
        generator.run(userId, img.getId(), prompt, refImage);
        return img.getId();
    }

    @Override
    public void setAvatar(Long userId, Long imageId) {
        OutfitImage img = outfitImageDao.selectById(imageId, userId);
        if (img == null || img.getStatus() == null || img.getStatus() != 1
                || !StringUtils.hasText(img.getImageKey())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "该效果图不存在或尚未生成完成");
        }
        bodyProfileDao.updateAvatar(userId, imageId);
    }

    /** 读参考图字节：根据 avatarImageId 定位效果图 imageKey，从存储读出 */
    private byte[] loadAvatar(Long userId, Long avatarImageId) {
        if (avatarImageId == null) {
            return null;
        }
        OutfitImage avatar = outfitImageDao.selectById(avatarImageId, userId);
        if (avatar == null || !StringUtils.hasText(avatar.getImageKey())) {
            return null;
        }
        return fileStorage.read(avatar.getImageKey());
    }

    @Override
    public OutfitImage status(Long userId, Long imageId) {
        OutfitImage img = outfitImageDao.selectById(imageId, userId);
        if (img == null) {
            throw new BizException(ResultCode.TASK_NOT_FOUND.getCode(), ResultCode.TASK_NOT_FOUND.getMsg());
        }
        return img;
    }

    @Override
    public PageResult<OutfitImage> history(Long userId, Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        long total = outfitImageDao.countByUser(userId);
        List<OutfitImage> list = outfitImageDao.listByUser(userId, (pn - 1) * ps, ps);
        return PageResult.of(list, total, pn, ps);
    }

    @Override
    public byte[] readImage(Long userId, Long imageId) {
        OutfitImage img = outfitImageDao.selectById(imageId, userId);
        if (img == null || img.getStatus() == null || img.getStatus() != 1
                || !StringUtils.hasText(img.getImageKey())) {
            return null;
        }
        return fileStorage.read(img.getImageKey());
    }

    /* ================= 私有方法 ================= */

    /** 取推荐记录：优先指定 id，否则取最近一条 */
    private OutfitRecord resolveRecord(Long userId, Long outfitRecordId) {
        if (outfitRecordId != null) {
            return outfitRecordDao.selectById(outfitRecordId, userId);
        }
        List<OutfitRecord> list = outfitRecordDao.listByUser(userId, 0, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 拼生图 prompt：三视图 + 身体数据 + 穿搭衣物 */
    private String buildPrompt(BodyProfile body, OutfitRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("3D 游戏角色建模三视图（正面、侧面、背面三个视角并排站立），同一角色，自然站姿，")
          .append("浅灰纯色背景，柔和均匀光照，高细节。");
        sb.append("角色：").append(body.getGender() != null && body.getGender() == 2 ? "女性" : "男性");
        if (body.getHeight() != null) {
            sb.append("，身高").append(plain(body.getHeight())).append("cm");
        }
        if (body.getWeight() != null) {
            sb.append("，体重").append(plain(body.getWeight())).append("kg");
        }
        if (body.getBust() != null || body.getWaist() != null || body.getHip() != null) {
            sb.append("，三围")
              .append(plain(body.getBust())).append("/")
              .append(plain(body.getWaist())).append("/")
              .append(plain(body.getHip())).append("cm");
        }
        if (StringUtils.hasText(body.getBodyType())) {
            sb.append("，").append(body.getBodyType()).append("体型");
        }
        sb.append("。穿着：");
        List<String> wear = parseItems(record.getItemsJson());
        sb.append(wear.isEmpty() ? "一套简洁得体的日常穿搭" : String.join(" + ", wear)).append("。");
        return sb.toString();
    }

    /** 从 itemsJson 快照解析「名称（颜色）」列表 */
    private List<String> parseItems(String itemsJson) {
        List<String> wear = new ArrayList<>();
        if (!StringUtils.hasText(itemsJson)) {
            return wear;
        }
        try {
            JsonNode arr = objectMapper.readTree(itemsJson);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    String name = node.path("name").asText("");
                    String color = node.path("color").asText("");
                    if (!name.isEmpty()) {
                        wear.add(color.isEmpty() ? name : name + "（" + color + "）");
                    }
                }
            }
        } catch (Exception ignored) {
            // 快照解析失败不影响主流程
        }
        return wear;
    }

    /** BigDecimal 去尾零转字符串（null 返回 "-"） */
    private String plain(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }
}
