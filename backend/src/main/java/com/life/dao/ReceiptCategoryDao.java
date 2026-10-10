package com.life.dao;

import com.life.domain.ReceiptCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 凭证分类字典数据访问接口 */
public interface ReceiptCategoryDao {

    /** 全部启用分类（按 sort 升序），用于首个页面的分类导览 */
    List<ReceiptCategory> listEnabled();

    ReceiptCategory selectById(@Param("id") Long id);

    ReceiptCategory selectByCode(@Param("code") String code);
}
