package com.smartpark.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ImportTaskVO", description = "导入任务状态视图对象")
public class ImportTaskVO {

    @Schema(description = "任务唯一标识")
    private String taskId;

    @Schema(description = "任务状态：PENDING-排队中、PROCESSING-处理中、SUCCESS-成功、FAIL-失败")
    private String status;

    @Schema(description = "总行数（不含表头）")
    private Integer totalRows;

    @Schema(description = "成功导入行数")
    private Integer successRows;

    @Schema(description = "失败行数")
    private Integer failRows;

    @Schema(description = "失败行明细")
    private List<RowError> failDetail;

    @Schema(description = "处理耗时（毫秒）")
    private Long costTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RowError", description = "行错误信息")
    public static class RowError {

        @Schema(description = "Excel行号")
        private Integer row;

        @Schema(description = "字段名")
        private String field;

        @Schema(description = "原始值")
        private String value;

        @Schema(description = "错误原因")
        private String reason;
    }
}
