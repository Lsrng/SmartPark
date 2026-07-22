package com.smartpark.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("user")
@Schema(name = "User", description = "用户实体")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(
            description = "用户名",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @Schema(
            description = "密码（加密存储）",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    @TableField(fill = FieldFill.INSERT)
    @Schema(
            description = "创建时间"
    )
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(
            description = "更新时间"
    )
    private LocalDateTime updatedAt;
}
