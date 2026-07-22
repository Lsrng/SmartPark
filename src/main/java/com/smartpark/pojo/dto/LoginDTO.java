package com.smartpark.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "LoginDTO", description = "用户登录请求体")
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(
            description = "用户名",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(
            description = "密码",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;
}
