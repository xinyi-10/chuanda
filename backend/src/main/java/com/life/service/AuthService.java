package com.life.service;

import com.life.domain.SysUser;
import org.springframework.web.multipart.MultipartFile;

/** 鉴权服务 */
public interface AuthService {

    /** 注册并直接签发 token（注册即登录） */
    String register(String username, String password, String nickname);

    /** 登录，成功返回 JWT token */
    String login(String username, String password);

    /** 按 id 查询当前用户（密码已脱敏） */
    SysUser getById(Long id);

    /** 上传头像（走统一文件存储），返回新的头像 key */
    String uploadAvatar(Long userId, MultipartFile file);
}
