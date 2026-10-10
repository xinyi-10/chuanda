package com.life.dao;

import com.life.domain.BodyProfile;
import org.apache.ibatis.annotations.Param;

/** 身体数据访问接口 */
public interface BodyProfileDao {

    /** 查当前用户的身体数据（未填写返回 null） */
    BodyProfile selectByUser(@Param("userId") Long userId);

    int insert(BodyProfile profile);

    int update(BodyProfile profile);

    /** 单独更新角色形象参考图（不影响其他身体数据） */
    int updateAvatar(@Param("userId") Long userId, @Param("avatarImageId") Long avatarImageId);
}
