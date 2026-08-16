package com.smartpark.validation.controller;

import com.smartpark.common.result.Result;
import com.smartpark.pojo.dto.StartRegisterRequest;
import com.smartpark.pojo.dto.StepSubmitRequest;
import com.smartpark.pojo.entity.EnterpriseRegister;
import com.smartpark.pojo.vo.ProgressVO;
import com.smartpark.validation.service.EnterpriseRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enterprise/register")
@RequiredArgsConstructor
@Tag(name = "企业入驻校验", description = "企业入驻校验相关接口")
public class EnterpriseRegisterController {

    private final EnterpriseRegisterService registerService;

    @PostMapping("/start")
    @Operation(summary = "创建入驻申请", description = "创建企业入驻申请，锁定当前配置版本快照")
    public Result<EnterpriseRegister> startRegister(@RequestBody StartRegisterRequest request) {
        EnterpriseRegister register = registerService.startRegister(
                request.getEnterpriseName(),
                request.getUnifiedCode(),
                request.getTypeId());
        return Result.success(register);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交校验步骤", description = "提交当前步骤的校验数据，通过后推进到下一步")
    public Result<ProgressVO> submitStep(@RequestBody StepSubmitRequest request) {
        ProgressVO progress = registerService.submitStep(
                request.getRegisterId(),
                request.getStepOrder(),
                request.getFormData(),
                null);
        return Result.success(progress);
    }

    @PostMapping("/back")
    @Operation(summary = "回退步骤", description = "将入驻流程回退到指定步骤")
    public Result<ProgressVO> backStep(
            @RequestParam Long registerId,
            @RequestParam Integer stepOrder) {
        ProgressVO progress = registerService.backStep(registerId, stepOrder, null);
        return Result.success(progress);
    }

    @GetMapping("/progress/{registerId}")
    @Operation(summary = "查询入驻进度", description = "查询指定入驻申请的当前进度和各步骤状态")
    public Result<ProgressVO> getProgress(@PathVariable Long registerId) {
        ProgressVO progress = registerService.getProgress(registerId);
        return Result.success(progress);
    }

    @PostMapping("/rollback/{registerId}")
    @Operation(summary = "回滚到指定步骤", description = "将入驻申请回滚到指定步骤，清除后续步骤数据")
    public Result<Void> rollbackTo(
            @PathVariable Long registerId,
            @RequestParam Integer targetStep) {
        registerService.rollbackTo(registerId, targetStep, null);
        return Result.success();
    }
}
