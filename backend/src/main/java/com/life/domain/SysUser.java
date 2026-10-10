package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 用户实体（对应表 sys_user） */
@Data
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    /** BCrypt 加密后的密码，切勿回传前端 */
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    /** 状态：1=启用 0=禁用 */
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0=正常 1=已删除 */
    private Integer deleted;
}
