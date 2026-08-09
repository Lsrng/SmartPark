package com.smartpark.upload.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UploadResult", description = "文件上传结果")
public class UploadResult {

    @Schema(description = "存储路径（bucket/uuid.ext）")
    private String storagePath;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "存储后的文件名（uuid.ext）")
    private String storedName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "真实扩展名")
    private String fileExtension;

    @Schema(description = "下载 URL")
    private String downloadUrl;
}
