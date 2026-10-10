package com.life.service.impl;

import com.life.common.BizException;
import com.life.common.ResultCode;
import com.life.dao.SysUserDao;
import com.life.domain.SysUser;
import com.life.service.AuthService;
import com.life.service.LedgerService;
import com.life.storage.FileStorage;
import com.life.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** 鉴权服务实现 */
@Service
public class AuthServiceImpl implements AuthService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private JwtUtil jwtUtil;

    /** 文件存储（本地 / RustFS），头像与凭证共用 */
    @Autowired
    private FileStorage fileStorage;

    /** 记账服务：注册时为新用户初始化默认收支分类 */
    @Autowired
    private LedgerService ledgerService;

    @Override
    public String register(String username, String password, String nickname) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "用户名和密码不能为空");
        }
        if (sysUserDao.selectByUsername(username) != null) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        // 密码 BCrypt 加密存储，绝不明文落库
        user.setPassword(encoder.encode(password));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setStatus(1);
        sysUserDao.insert(user);
        // 为新用户初始化默认收支分类，保证注册后立刻能记账
        ledgerService.initDefaultCategories(user.getId());
        return jwtUtil.generate(user.getId(), user.getUsername());
    }

    @Override
    public String login(String username, String password) {
        SysUser user = sysUserDao.selectByUsername(username);
        // 用户不存在与密码错误返回同一提示，避免用户名枚举
        if (user == null || !encoder.matches(password, user.getPassword())) {
            throw new BizException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        sysUserDao.updateLastLoginTime(user.getId());
        return jwtUtil.generate(user.getId(), user.getUsername());
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = sysUserDao.selectById(id);
        if (user != null) {
            // 密码脱敏，禁止回传前端
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择头像图片");
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "png";
        try {
            byte[] content = file.getBytes();
            String key = "avatar/" + userId + "/"
                    + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            fileStorage.save(key, content, file.getContentType());
            sysUserDao.updateAvatar(userId, key);
            return key;
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "头像上传失败：" + e.getMessage());
        }
    }
}
