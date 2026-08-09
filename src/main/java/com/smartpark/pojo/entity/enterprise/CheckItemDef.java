package com.smartpark.pojo.entity.enterprise;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("check_item_def")
@Schema(name = "CheckItemDef", description = "校验项定义")
public class CheckItemDef implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "校验项名称")
    private String name;

    @Schema(description = "校验项编码")
    private String code;

    @Schema(description = "对应的Spring Bean名称")
    private String handlerBean;

    @Schema(description = "校验项描述")
    private String description;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：ENABLED-启用、DISABLED-禁用")
    private String status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
