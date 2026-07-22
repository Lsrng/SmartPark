package com.smartpark.controller;

import com.smartpark.common.result.Result;
import com.smartpark.pojo.dto.DeviceDataReportDTO;
import com.smartpark.service.DeviceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "物联设备管理", description = "物联设备数据上报相关接口")
@RequestMapping("/device")
public class DeviceDataController {

    private final DeviceDataService deviceDataService;

    @PostMapping("/data/report")
    @Operation(
            summary = "上报设备数据",
            description = "物联设备上报监测数据，每个设备每分钟限流 1 次"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上报成功"),
            @ApiResponse(responseCode = "429", description = "请求频率过高，被限流"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public Result<Void> report(@Valid @RequestBody DeviceDataReportDTO dto) {
        log.info("收到设备上报请求 - 设备ID: {}", dto.getDeviceId());
        deviceDataService.report(dto);
        return Result.success("上报成功");
    }
}
