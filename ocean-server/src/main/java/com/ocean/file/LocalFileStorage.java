package com.ocean.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件存储实现
 * <p>
 * 开发环境使用，文件存储在本地磁盘的 upload-path 目录下。
 * 通过 /files/** 静态资源映射对外提供访问。
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "ocean.file.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorageStrategy {

    @Value("${ocean.file.local.upload-path:./upload/}")
    private String uploadPath;

    private Path uploadDir;

    @PostConstruct
    public void init() {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            log.info("本地文件存储目录: {}", uploadDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", uploadDir, e);
        }
    }

    @Override
    public String upload(byte[] data, String fileName) {
        try {
            // 确保子目录存在
            File dest = new File(uploadDir.toFile(), fileName);
            File parentDir = dest.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(data);
                fos.flush();
            }

            log.debug("文件上传成功: {}", fileName);
            return getUrl(fileName);
        } catch (IOException e) {
            log.error("本地文件上传失败: {}", fileName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileName) {
        try {
            File file = new File(uploadDir.toFile(), fileName);
            if (file.exists()) {
                if (file.delete()) {
                    log.debug("文件删除成功: {}", fileName);
                } else {
                    log.warn("文件删除失败: {}", fileName);
                }
            }
        } catch (Exception e) {
            log.error("本地文件删除失败: {}", fileName, e);
        }
    }

    @Override
    public String getUrl(String fileName) {
        return "/files/" + fileName;
    }
}
