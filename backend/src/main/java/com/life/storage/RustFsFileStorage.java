package com.life.storage;

import com.life.common.BizException;
import com.life.common.ResultCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * RustFS（S3 兼容）对象存储实现。
 *
 * <p>用 MinIO SDK 作为客户端（比 AWS SDK 轻量，且对 S3 兼容服务适配最好）。
 * 凭证与地址来自 {@code app.properties}：storage.endpoint / access-key / secret-key / bucket。</p>
 */
@Component("rustFsFileStorage")
public class RustFsFileStorage implements FileStorage, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RustFsFileStorage.class);

    @Value("${storage.endpoint:http://127.0.0.1:9000}")
    private String endpoint;

    @Value("${storage.access-key:rustfsadmin}")
    private String accessKey;

    @Value("${storage.secret-key:}")
    private String secretKey;

    @Value("${storage.bucket:chuanda}")
    private String bucket;

    @Value("${storage.auto-create-bucket:true}")
    private boolean autoCreateBucket;

    private MinioClient client;

    @Override
    public void afterPropertiesSet() {
        client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        if (autoCreateBucket) {
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("[storage] 已创建 bucket: {}", bucket);
                } else {
                    log.info("[storage] bucket 已存在: {}", bucket);
                }
            } catch (Exception e) {
                // 启动阶段不阻断：真正上传时再抛出明确错误
                log.warn("[storage] RustFS 连接/bucket 初始化失败（{}）：{}", endpoint, e.getMessage());
            }
        }
    }

    @Override
    public String type() {
        return "rustfs";
    }

    @Override
    public String save(String key, byte[] content, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
            return key;
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "对象存储上传失败：" + e.getMessage());
        }
    }

    @Override
    public byte[] read(String key) {
        try (InputStream in = client.getObject(GetObjectArgs.builder()
                .bucket(bucket).object(key).build())) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            log.warn("[storage] 读取对象失败 {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            log.warn("[storage] 删除对象失败 {}: {}", key, e.getMessage());
        }
    }
}
