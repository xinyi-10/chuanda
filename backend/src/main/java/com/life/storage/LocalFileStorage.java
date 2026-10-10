package com.life.storage;

import com.life.common.BizException;
import com.life.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** 本地磁盘存储实现（开发默认） */
@Component("localFileStorage")
public class LocalFileStorage implements FileStorage {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public String type() {
        return "local";
    }

    @Override
    public String save(String key, byte[] content, String contentType) {
        try {
            File target = new File(uploadDir, key);
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("目录创建失败: " + parent.getAbsolutePath());
            }
            Files.write(target.toPath(), content);
            return key;
        } catch (IOException e) {
            throw new BizException(ResultCode.FAIL.getCode(), "本地保存失败：" + e.getMessage());
        }
    }

    @Override
    public byte[] read(String key) {
        try {
            File target = new File(uploadDir, key);
            if (!target.exists()) {
                return null;
            }
            return Files.readAllBytes(target.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        try {
            File target = new File(uploadDir, key);
            if (target.exists()) {
                target.delete();
            }
        } catch (Exception ignored) {
            // 删除失败不影响主流程
        }
    }
}
