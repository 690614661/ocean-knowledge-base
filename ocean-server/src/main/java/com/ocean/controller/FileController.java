package com.ocean.controller;

import com.ocean.common.BusinessException;
import com.ocean.common.CommonResp;
import com.ocean.file.FileStorageStrategy;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Api(tags = "文件上传")
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileStorageStrategy fileStorage;

    @Value("${ocean.file.max-size:10485760}")
    private long maxSize;

    @Value("${ocean.file.allowed-types:}")
    private String allowedTypesStr;

    @ApiOperation("通用文件上传")
    @PostMapping("/upload")
    public CommonResp<String> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(defaultValue = "file") String dir) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择文件");
        }

        // 校验文件大小
        if (file.getSize() > maxSize) {
            long mb = maxSize / (1024 * 1024);
            throw new BusinessException("文件大小不能超过" + mb + "MB");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType != null && !allowedTypesStr.isEmpty()) {
            List<String> allowedTypes = Arrays.asList(allowedTypesStr.split("\\s*,\\s*"));
            if (allowedTypes.stream().noneMatch(t -> t.equalsIgnoreCase(contentType))) {
                throw new BusinessException("不支持的文件类型: " + contentType);
            }
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = dir + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        // 上传（自动切换到 local 或 qiniu）
        String url;
        try {
            url = fileStorage.upload(file.getBytes(), fileName);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        return CommonResp.ok("上传成功", url);
    }
}
