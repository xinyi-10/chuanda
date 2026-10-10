package com.life.dao;

import com.life.domain.SysUser;
import org.apache.ibatis.annotations.Param;

/**
 * 用户数据访问接口（对应 mapper/SysUserMapper.xml）。
 *
 * <p>包命名沿既有项目风格：entity→domain、mapper→dao。</p>
 */
public interface SysUserDao {

    /** 按用户名查询（用于登录、注册查重），仅取未删除记录 */
    SysUser selectByUsername(@Param("username") String username);

    /** 按主键查询 */
    SysUser selectById(@Param("id") Long id);

    /** 新增用户，成功后回填自增 id */
    int insert(SysUser user);

    /** 更新最后登录时间 */
    int updateLastLoginTime(@Param("id") Long id);

    /** 更新头像（存对象存储的 key） */
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);
}
