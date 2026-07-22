package com.smartpark.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("meeting_rooms")
@Schema(name = "MeetingRoom", description = "会议室实体")
public class MeetingRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "room_id", type = IdType.AUTO)
    private Integer roomId;

    @Schema(
            description = "会议室名称",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Schema(
            description = "会议室位置",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String location;

    @Schema(
            description = "会议室容量",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer capacity;

    @Schema(
            description = "会议室设备配置"
    )
    private String equipment;

    @Schema(
            description = "会议室状态(关联sys_meeting_room_status)"
    )
    private Integer status;

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