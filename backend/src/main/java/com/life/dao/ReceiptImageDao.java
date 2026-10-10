package com.life.dao;

import com.life.domain.ReceiptImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 支付凭证图片数据访问接口 */
public interface ReceiptImageDao {

    /** 按 MD5 查同用户下的重复记录（精确判重，未删除） */
    ReceiptImage selectByMd5(@Param("userId") Long userId, @Param("md5") String md5);

    /** 取该用户全部未删除记录的 id + phash（近似判重比对用） */
    List<ReceiptImage> listFingerprints(@Param("userId") Long userId);

    /** 分页查询（可按分类 / 审核状态过滤） */
    List<ReceiptImage> listByPage(@Param("userId") Long userId,
                                  @Param("categoryId") Long categoryId,
                                  @Param("auditStatus") Integer auditStatus,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    long countByCondition(@Param("userId") Long userId,
                          @Param("categoryId") Long categoryId,
                          @Param("auditStatus") Integer auditStatus);

    ReceiptImage selectById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(ReceiptImage image);

    /** 补录 / 修正时间与金额 */
    int updateTimeAndAmount(ReceiptImage image);

    /** 修正分类（人工纠正自动归类结果） */
    int updateCategory(@Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("categoryId") Long categoryId);

    int logicDelete(@Param("id") Long id, @Param("userId") Long userId);
}
