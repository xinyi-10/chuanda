package com.life.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 存储路由器（@Primary）：业务只注入 {@link FileStorage}，由本类按配置决定走哪个实现。
 *
 * <p>{@code app.properties} 的 {@code storage.type=local|rustfs} 切换，
 * 改配置重启即可，业务代码零改动。</p>
 */
@Component
@Primary
public class FileStorageRouter implements FileStorage {

    @Value("${storage.type:local}")
    private String storageType;

    @Autowired
    @Qualifier("localFileStorage")
    private FileStorage localFileStorage;

    @Autowired
    @Qualifier("rustFsFileStorage")
    private FileStorage rustFsFileStorage;

    private FileStorage target() {
        return "rustfs".equalsIgnoreCase(storageType) ? rustFsFileStorage : localFileStorage;
    }

    @Override
    public String type() {
        return target().type();
    }

    @Override
    public String save(String key, byte[] content, String contentType) {
        return target().save(key, content, contentType);
    }

    @Override
    public byte[] read(String key) {
        return target().read(key);
    }

    @Override
    public void delete(String key) {
        target().delete(key);
    }
}
