package com.smartpark.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CheckResultDTO", description = "校验结果DTO")
public class CheckResultDTO {

    @Schema(description = "是否通过")
    private Boolean passed;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "校验详情")
    private Map<String, Object> detail;

    public static CheckResultDTO pass(Map<String, Object> detail) {
        return CheckResultDTO.builder()
                .passed(true)
                .detail(detail)
                .build();
    }

    public static CheckResultDTO fail(String errorCode, String errorMessage) {
        return CheckResultDTO.builder()
                .passed(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    public static CheckResultDTO fail(String errorMessage) {
        return fail(null, errorMessage);
    }
}
