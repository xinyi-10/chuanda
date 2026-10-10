package com.life.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.life.common.BizException;
import com.life.common.PageResult;
import com.life.common.ResultCode;
import com.life.dao.BodyProfileDao;
import com.life.dao.ClothingItemDao;
import com.life.dao.OutfitRecordDao;
import com.life.domain.BodyProfile;
import com.life.domain.ClothingItem;
import com.life.domain.OutfitRecord;
import com.life.domain.vo.OutfitSuggestVO;
import com.life.domain.vo.ClothingRecognitionResult;
import com.life.domain.vo.WeatherInfo;
import com.life.service.OutfitService;
import com.life.storage.FileStorage;
import com.life.util.ClothingRecognitionClient;
import com.life.util.WeatherClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** 穿搭推荐服务实现（规则引擎，无外部依赖也能跑） */
@Service
public class OutfitServiceImpl implements OutfitService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 随机挑选衣物（「换一批」用），同一候选池内随机出不同的搭配 */
    private final Random random = new Random();

    private static final String[] CATEGORY_LABEL = {"top:上装", "bottom:下装", "outer:外套", "shoes:鞋", "accessory:配饰"};

    @Autowired
    private BodyProfileDao bodyProfileDao;

    @Autowired
    private ClothingItemDao clothingItemDao;

    @Autowired
    private OutfitRecordDao outfitRecordDao;

    @Autowired
    private WeatherClient weatherClient;

    /** 衣物图片识别（MiniMax 视觉模型） */
    @Autowired
    private ClothingRecognitionClient clothingRecognitionClient;

    /** 文件存储（本地 / RustFS），衣物图片与凭证、头像共用 */
    @Autowired
    private FileStorage fileStorage;

    /* ================= 身体数据 ================= */

    @Override
    public BodyProfile getBody(Long userId) {
        BodyProfile p = bodyProfileDao.selectByUser(userId);
        return p == null ? null : enrich(p);
    }

    @Override
    public BodyProfile saveBody(Long userId, BodyProfile profile) {
        if (profile.getHeight() == null || profile.getHeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请填写身高");
        }
        if (profile.getWeight() == null || profile.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请填写体重");
        }
        profile.setUserId(userId);
        BodyProfile exist = bodyProfileDao.selectByUser(userId);
        if (exist == null) {
            bodyProfileDao.insert(profile);
        } else {
            profile.setId(exist.getId());
            bodyProfileDao.update(profile);
        }
        return enrich(bodyProfileDao.selectByUser(userId));
    }

    /* ================= 衣物库 ================= */

    @Override
    public List<ClothingItem> listClothing(Long userId, String category, String season, String style) {
        List<ClothingItem> list = clothingItemDao.listByUser(userId, category, season, style);
        for (ClothingItem c : list) {
            c.setCategoryLabel(labelOf(c.getCategory()));
        }
        return list;
    }

    @Override
    public Long saveClothing(Long userId, ClothingItem item) {
        if (!StringUtils.hasText(item.getName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请填写衣物名称");
        }
        if (!StringUtils.hasText(item.getCategory())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择衣物类别");
        }
        item.setUserId(userId);
        if (item.getId() == null) {
            clothingItemDao.insert(item);
        } else {
            clothingItemDao.update(item);
        }
        return item.getId();
    }

    @Override
    public String uploadClothingImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择衣物图片");
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "png";
        try {
            byte[] content = file.getBytes();
            String key = "clothing/" + userId + "/"
                    + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            fileStorage.save(key, content, file.getContentType());
            return key;
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "衣物图片上传失败：" + e.getMessage());
        }
    }

    @Override
    public ClothingRecognitionResult recognizeClothing(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择衣物图片");
        }
        try {
            return clothingRecognitionClient.recognize(file.getBytes());
        } catch (Exception e) {
            ClothingRecognitionResult r = new ClothingRecognitionResult();
            r.setRecognized(false);
            return r;
        }
    }

    @Override
    public byte[] readClothingImage(Long userId, Long clothingId) {
        ClothingItem item = clothingItemDao.selectById(clothingId, userId);
        if (item == null || !StringUtils.hasText(item.getImageUrl())) {
            return null;
        }
        return fileStorage.read(item.getImageUrl());
    }

    @Override
    public void deleteClothing(Long userId, Long id) {
        clothingItemDao.logicDelete(id, userId);
    }

    /* ================= 推荐 ================= */

    @Override
    public OutfitSuggestVO recommend(Long userId, String occasion) {
        BodyProfile body = getBody(userId);
        if (body == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请先填写身高体重等身体数据");
        }

        WeatherInfo weather = weatherClient.getWeather(body.getCity());
        int temp = weather.getTemp() == null ? 20 : weather.getTemp().intValue();
        String cond = weather.getCondition() == null ? "" : weather.getCondition();
        boolean rainy = cond.contains("雨") || cond.contains("雪")
                || cond.toLowerCase().contains("rain") || cond.toLowerCase().contains("snow");

        List<ClothingItem> wardrobe = clothingItemDao.listByUser(userId, null, null, null);
        String seasonKey = seasonKeyOf(temp);
        String styleKey = styleKeyOf(occasion, body.getStylePref());

        // 需要哪些类别
        List<String> needCats = new ArrayList<>();
        needCats.add("top");
        needCats.add("bottom");
        if (temp <= 20) {
            needCats.add("outer");
        }
        needCats.add("shoes");

        List<ClothingItem> picked = new ArrayList<>();
        for (String cat : needCats) {
            ClothingItem c = pick(wardrobe, cat, seasonKey, styleKey);
            if (c != null) {
                picked.add(c);
            }
        }

        // 推荐理由
        List<String> reasons = new ArrayList<>();
        reasons.add(describeWeather(weather, temp, cond));
        if (temp <= 10) {
            reasons.add("气温偏低 → 需要保暖外套，内搭建议厚款");
        } else if (temp <= 20) {
            reasons.add("早晚偏凉 → 建议加一件外套，方便穿脱");
        } else if (temp <= 27) {
            reasons.add("温度舒适 → 长袖或薄款即可");
        } else {
            reasons.add("天气炎热 → 选择透气轻薄的短袖与下装");
        }
        if (rainy) {
            reasons.add("有降水 → 建议防水鞋 + 随身带伞，下装避免浅色长裤");
        }
        if (StringUtils.hasText(body.getBodyType())) {
            reasons.add("体型「" + body.getBodyType() + "」（BMI " + body.getBmi() + "）→ " + body.getBodyTip());
        }
        if (picked.isEmpty()) {
            reasons.add("衣物库还是空的 → 先去「衣物库」添加几件，推荐会更精准");
        }

        String advice = buildAdvice(weather, picked, rainy, temp);

        OutfitRecord record = new OutfitRecord();
        record.setUserId(userId);
        record.setOccasion(occasion);
        record.setWeather((weather.getCity() == null ? "" : weather.getCity())
                + " " + temp + "℃ " + cond);
        record.setItemsJson(toJson(picked));
        record.setAdvice(advice);
        record.setSource(2);
        outfitRecordDao.insert(record);

        OutfitSuggestVO vo = new OutfitSuggestVO();
        vo.setRecordId(record.getId());
        vo.setWeather(weather);
        vo.setItems(picked);
        vo.setReasons(reasons);
        vo.setAdvice(advice);
        vo.setSource(2);
        vo.setBodyTip(body.getBodyTip());
        return vo;
    }

    @Override
    public PageResult<OutfitRecord> history(Long userId, Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        long total = outfitRecordDao.countByUser(userId);
        List<OutfitRecord> list = outfitRecordDao.listByUser(userId, (pn - 1) * ps, ps);
        return PageResult.of(list, total, pn, ps);
    }

    /* ================= 私有方法 ================= */

    /** 计算 BMI 与体型标签 */
    private BodyProfile enrich(BodyProfile p) {
        if (p.getHeight() != null && p.getWeight() != null
                && p.getHeight().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal h = p.getHeight().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal bmi = p.getWeight().divide(h.multiply(h), 1, RoundingMode.HALF_UP);
            p.setBmi(bmi);
            double v = bmi.doubleValue();
            if (v < 18.5) {
                p.setBodyType("偏瘦");
                p.setBodyTip("可用层次叠穿增加视觉分量，避免过于宽松显得单薄");
            } else if (v < 24) {
                p.setBodyType("标准");
                p.setBodyTip("版型选择自由，修身或直筒都能驾驭");
            } else if (v < 28) {
                p.setBodyType("偏胖");
                p.setBodyTip("建议深色系 + 直筒版型，避免紧身与横条纹");
            } else {
                p.setBodyType("偏胖");
                p.setBodyTip("优先深色、垂坠面料，V 领与竖向线条更显瘦");
            }
        }
        return p;
    }

    private String labelOf(String category) {
        for (String kv : CATEGORY_LABEL) {
            String[] parts = kv.split(":");
            if (parts[0].equals(category)) {
                return parts[1];
            }
        }
        return category;
    }

    /** 温度 → 字典季节编码 */
    private String seasonKeyOf(int temp) {
        if (temp >= 28) {
            return "summer";
        }
        if (temp >= 21) {
            return "spring";
        }
        if (temp >= 11) {
            return "autumn";
        }
        return "winter";
    }

    /** 场合 → 风格编码（用户偏好兜底） */
    private String styleKeyOf(String occasion, String pref) {
        if (StringUtils.hasText(occasion)) {
            if (occasion.contains("上班") || occasion.contains("商务") || occasion.contains("面试")) {
                return "business";
            }
            if (occasion.contains("正式") || occasion.contains("宴会")) {
                return "formal";
            }
            if (occasion.contains("运动") || occasion.contains("健身")) {
                return "sport";
            }
        }
        return StringUtils.hasText(pref) ? pref : "casual";
    }

    /** 挑衣物：风格+季节 > 季节 > 任意该类；同一优先级内随机选，支持「换一批」 */
    private ClothingItem pick(List<ClothingItem> wardrobe, String category, String season, String style) {
        List<ClothingItem> bySeasonAndStyle = new ArrayList<>();
        List<ClothingItem> bySeason = new ArrayList<>();
        List<ClothingItem> any = new ArrayList<>();
        for (ClothingItem c : wardrobe) {
            if (!category.equalsIgnoreCase(c.getCategory())) {
                continue;
            }
            any.add(c);
            boolean seasonOk = season.equalsIgnoreCase(c.getSeason()) || "all".equalsIgnoreCase(c.getSeason());
            if (seasonOk) {
                bySeason.add(c);
                if (style.equalsIgnoreCase(c.getStyle())) {
                    bySeasonAndStyle.add(c);
                }
            }
        }
        List<ClothingItem> pool = !bySeasonAndStyle.isEmpty() ? bySeasonAndStyle
                : !bySeason.isEmpty() ? bySeason : any;
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private String describeWeather(WeatherInfo w, int temp, String cond) {
        StringBuilder sb = new StringBuilder();
        sb.append(w.getCity() == null ? "当地" : w.getCity()).append(" 当前 ").append(temp).append("℃");
        if (StringUtils.hasText(cond) && !"未知".equals(cond)) {
            sb.append(" ").append(cond);
        }
        if (!w.isFromApi()) {
            sb.append("（天气服务暂不可用，已按季节估算）");
        }
        return sb.toString();
    }

    private String buildAdvice(WeatherInfo weather, List<ClothingItem> picked, boolean rainy, int temp) {
        if (picked.isEmpty()) {
            return "衣物库为空，暂时给不出具体搭配。当前气温 " + temp
                    + "℃，建议按季节穿应季衣物，并到「衣物库」添加几件常用单品。";
        }
        StringBuilder sb = new StringBuilder("今天推荐：");
        for (int i = 0; i < picked.size(); i++) {
            ClothingItem c = picked.get(i);
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(c.getName());
            if (StringUtils.hasText(c.getColor())) {
                sb.append("（").append(c.getColor()).append("）");
            }
        }
        sb.append("。");
        if (rainy) {
            sb.append("记得带伞，鞋子选防水款。");
        }
        return sb.toString();
    }

    private String toJson(List<ClothingItem> items) {
        try {
            List<ClothingItem> snapshots = new ArrayList<>();
            for (ClothingItem c : items) {
                ClothingItem s = new ClothingItem();
                s.setId(c.getId());
                s.setName(c.getName());
                s.setCategory(c.getCategory());
                s.setColor(c.getColor());
                snapshots.add(s);
            }
            return objectMapper.writeValueAsString(snapshots);
        } catch (Exception e) {
            return "[]";
        }
    }
}
