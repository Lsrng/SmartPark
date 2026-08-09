package com.smartpark.upload.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_file_metadata")
@Schema(name = "FileMetadata", description = "文件元信息")
public class FileMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "存储路径（bucket/yyyy/MM/dd/uuid.ext）")
    private String storagePath;

    @Schema(description = "业务分组")
    private String bucket;

    @Schema(description = "存储文件名（uuid.ext）")
    private String storedName;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "魔数检测的真实扩展名")
    private String realExtension;

    @Schema(description = "真实 MIME 类型")
    private String mimeType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;
}
