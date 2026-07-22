package com.smartpark.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "DeviceDataReportDTO", description = "物联设备数据上报请求体")
public class DeviceDataReportDTO {

    @NotNull(message = "设备ID不能为空")
    @Schema(
            description = "设备ID，唯一标识一个物联设备",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1"
    )
    private Long deviceId;

    @NotNull(message = "上报数据不能为空")
    @Schema(
            description = "上报数据内容（JSON格式）",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "{\"temperature\": 25.6, \"humidity\": 60, \"status\": \"normal\"}"
    )
    private Object data;

    @Schema(
            description = "设备上报时间戳，不传则使用服务器当前时间",
            example = "2026-07-20T10:30:00"
    )
    private LocalDateTime reportTime;
}
