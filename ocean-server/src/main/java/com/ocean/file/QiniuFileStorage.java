package com.ocean.file;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ocean.common.BusinessException;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 七牛云对象存储实现
 * <p>
 * 生产环境使用，文件存储在七牛云 Bucket 中，通过 CDN 加速访问。
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "ocean.file.storage-type", havingValue = "qiniu")
public class QiniuFileStorage implements FileStorageStrategy {

    @Value("${ocean.file.qiniu.access-key:}")
    private String accessKey;

    @Value("${ocean.file.qiniu.secret-key:}")
    private String secretKey;

    @Value("${ocean.file.qiniu.bucket:ocean-knowledge}")
    private String bucket;

    @Value("${ocean.file.qiniu.domain:}")
    private String domain;

    @Value("${ocean.file.qiniu.region:}")
    private String region;

    private UploadManager uploadManager;
    private Auth auth;
    private BucketManager bucketManager;

    @PostConstruct
    public void init() {
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            log.warn("七牛云未配置 AccessKey/SecretKey，上传将在运行时失败");
            return;
        }
        Region r;
        if ("southchina".equalsIgnoreCase(region)) {
            r = Region.region2(); // 华南(广州)
        } else if ("northchina".equalsIgnoreCase(region)) {
            r = Region.region1(); // 华北(北京)
        } else if ("eastchina".equalsIgnoreCase(region)) {
            r = Region.region0(); // 华东(上海)
        } else {
            r = Region.autoRegion();
        }
        Configuration cfg = new Configuration(r);
        uploadManager = new UploadManager(cfg);
        auth = Auth.create(accessKey, secretKey);
        bucketManager = new BucketManager(auth, cfg);
        log.info("七牛云存储初始化完成, bucket={}, domain={}, region={}", bucket, domain, region);
    }

    @Override
    public String upload(byte[] data, String fileName) {
        if (auth == null) {
            throw new BusinessException("七牛云未配置，请先设置 QINIU_ACCESS_KEY 和 QINIU_SECRET_KEY");
        }
        String upToken = auth.uploadToken(bucket);
        try {
            Response response = uploadManager.put(data, fileName, upToken);
            JSONObject putRet = JSON.parseObject(response.bodyString());
            String key = putRet.getString("key");
            String url = getUrl(key);
            log.debug("七牛云上传成功: {} -> {}", fileName, url);
            return url;
        } catch (QiniuException ex) {
            log.error("七牛云上传失败: {}", fileName, ex);
            throw new BusinessException("文件上传到七牛云失败: " + ex.getMessage());
        }
    }

    @Override
    public void delete(String fileName) {
        if (bucketManager == null) return;
        try {
            bucketManager.delete(bucket, fileName);
            log.debug("七牛云删除成功: {}", fileName);
        } catch (QiniuException ex) {
            if (ex.code() != 612) {
                log.error("七牛云删除失败: {}", fileName, ex);
                throw new BusinessException("删除文件失败: " + ex.getMessage());
            }
        }
    }

    @Override
    public String getUrl(String fileName) {
        if (domain == null || domain.isEmpty()) {
            return "/files/" + fileName;
        }
        String baseDomain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        return baseDomain + "/" + fileName;
    }
}
