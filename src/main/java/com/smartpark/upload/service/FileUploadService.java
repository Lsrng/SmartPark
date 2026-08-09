package com.smartpark.upload.service;

import com.smartpark.upload.pojo.dto.UploadResult;
import com.smartpark.upload.pojo.vo.UploadFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件上传服务接口
 */
public interface FileUploadService {

    /**
     * 单文件上传
     *
     * @param file  上传文件
     * @param bucket 业务分组
     * @return 上传结果
     */
    UploadResult upload(MultipartFile file, String bucket);

    /**
     * 文件下载
     *
     * @param storagePath 存储路径
     * @return 文件信息（含流）
     */
    DownloadResult download(String storagePath);

    /**
     * 文件删除
     *
     * @param storagePath 存储路径
     */
    void delete(String storagePath);

    /**
     * 下载结果封装
     */
    class DownloadResult {
        private final byte[] fileData;
        private final String mimeType;
        private final String storedName;

        public DownloadResult(byte[] fileData, String mimeType, String storedName) {
            this.fileData = fileData;
            this.mimeType = mimeType;
            this.storedName = storedName;
        }

        public byte[] getFileData() { return fileData; }
        public String getMimeType() { return mimeType; }
        public String getStoredName() { return storedName; }
    }
}
