package com.ocean.file;

/**
 * 文件存储策略接口
 * <p>
 * 所有存储后端统一实现此接口，业务层无感知。
 * 通过 @ConditionalOnProperty 根据配置切换 local / qiniu。
 */
public interface FileStorageStrategy {

    /**
     * 上传文件
     * @param data     文件字节数组
     * @param fileName 文件名（含目录，如 cover/ebook_xxx.jpg）
     * @return 完整可访问 URL
     */
    String upload(byte[] data, String fileName);

    /**
     * 删除文件
     * @param fileName 文件名（含目录）
     */
    void delete(String fileName);

    /**
     * 获取完整可访问 URL
     * @param fileName 文件名（含目录）
     * @return 完整 URL
     */
    String getUrl(String fileName);
}
