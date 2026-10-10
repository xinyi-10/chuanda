package com.life.storage;

/**
 * 文件存储抽象（可插拔）。
 *
 * <p>实现：{@link LocalFileStorage}（本地磁盘，开发默认）与
 * {@link RustFsFileStorage}（RustFS / S3 兼容对象存储）。
 * 通过 {@code app.properties} 的 {@code storage.type=local|rustfs} 切换，
 * 业务代码只依赖本接口，不感知底层存储。</p>
 *
 * <p>凭证图片与用户头像共用同一条存储链路。</p>
 */
public interface FileStorage {

    /** 存储类型标识：local / rustfs */
    String type();

    /**
     * 保存文件。
     *
     * @param key         存储路径（如 receipt/1/xxx.png）
     * @param content     文件字节
     * @param contentType MIME 类型
     * @return 实际存储路径
     */
    String save(String key, byte[] content, String contentType);

    /** 读取文件内容；不存在返回 null */
    byte[] read(String key);

    /** 删除文件（不存在时静默） */
    void delete(String key);
}
