package com.smartpark.upload.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UploadFileVO", description = "文件信息")
public class UploadFileVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "存储路径")
    private String storagePath;

    @Schema(description = "业务分组")
    private String bucket;

    @Schema(description = "存储文件名")
    private String storedName;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "真实扩展名")
    private String realExtension;

    @Schema(description = "真实 MIME 类型")
    private String mimeType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;
}
