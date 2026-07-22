package com.smartpark.pojo.entity;

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
@TableName("device_data_record")
@Schema(name = "DeviceDataRecord", description = "设备上报数据记录实体")
public class DeviceDataRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(
            description = "设备ID",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long deviceId;

    @Schema(
            description = "上报数据（JSON格式）"
    )
    private String data;

    @Schema(
            description = "设备上报时间"
    )
    private LocalDateTime reportTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "记录创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "记录更新时间")
    private LocalDateTime updatedAt;
}
