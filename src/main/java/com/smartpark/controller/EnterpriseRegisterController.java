package com.smartpark.controller;

import com.smartpark.common.context.BaseContext;
import com.smartpark.common.result.Result;
import com.smartpark.enterprise.handler.CheckResult;
import com.smartpark.pojo.dto.enterprise.StartRegisterRequest;
import com.smartpark.pojo.dto.enterprise.StepSaveRequest;
import com.smartpark.pojo.dto.enterprise.StepSubmitRequest;
import com.smartpark.pojo.vo.enterprise.EnterpriseTypeVO;
import com.smartpark.pojo.vo.enterprise.ProgressVO;
import com.smartpark.pojo.vo.enterprise.StepInfoVO;
import com.smartpark.service.EnterpriseRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "企业入驻", description = "企业入驻校验相关接口")
@RequestMapping("/enterprise")
public class EnterpriseRegisterController {

    private final EnterpriseRegisterService enterpriseRegisterService;

    @GetMapping("/types")
    @Operation(summary = "获取企业类型列表", description = "获取所有启用的企业类型及对应的校验步骤数")
    public Result<List<EnterpriseTypeVO>> getEnterpriseTypes() {
        return Result.success(enterpriseRegisterService.getEnterpriseTypes());
    }

    @PostMapping("/register/start")
    @Operation(summary = "开始入驻", description = "选择企业类型并填写基本信息，创建入驻申请草稿")
    public Result<Long> startRegister(@Valid @RequestBody StartRegisterRequest request) {
        Long userId = BaseContext.getCurrentId();
        Long registerId = enterpriseRegisterService.startRegister(request, userId);
        return Result.success("入驻申请创建成功，请进行第1步校验", registerId);
    }

    @GetMapping("/register/{id}/progress")
    @Operation(summary = "获取入驻进度", description = "获取入驻申请的整体进度，包括当前步骤、总步骤数、各步骤状态")
    public Result<ProgressVO> getProgress(@PathVariable("id") Long registerId) {
        return Result.success(enterpriseRegisterService.getProgress(registerId));
    }

    @GetMapping("/register/{id}/steps")
    @Operation(summary = "获取校验步骤列表", description = "获取该入驻申请的所有校验步骤信息")
    public Result<List<StepInfoVO>> getSteps(@PathVariable("id") Long registerId) {
        return Result.success(enterpriseRegisterService.getSteps(registerId));
    }

    @PostMapping("/register/step/save")
    @Operation(summary = "保存步骤草稿", description = "保存当前步骤填写的表单数据，支持中途退出后继续")
    public Result<Void> saveStepDraft(@Valid @RequestBody StepSaveRequest request) {
        enterpriseRegisterService.saveStepDraft(request);
        return Result.success("草稿保存成功");
    }

    @GetMapping("/register/{id}/step/{stepOrder}")
    @Operation(summary = "获取步骤草稿", description = "获取某步骤已保存的草稿数据")
    public Result<Map<String, Object>> getStepDraft(
            @PathVariable("id") Long registerId,
            @PathVariable("stepOrder") Integer stepOrder) {
        return Result.success(enterpriseRegisterService.getStepDraft(registerId, stepOrder));
    }

    @PostMapping("/register/step/submit")
    @Operation(summary = "提交步骤校验", description = "提交当前步骤进行校验，校验通过后自动进入下一步")
    public Result<CheckResult> submitStep(@Valid @RequestBody StepSubmitRequest request) {
        Long userId = BaseContext.getCurrentId();
        CheckResult result = enterpriseRegisterService.submitStep(request, userId);
        if (result.isPassed()) {
            return Result.success(result.getMessage(), result);
        } else {
            return Result.error(400, result.getMessage());
        }
    }

    @PostMapping("/register/{id}/step/back")
    @Operation(summary = "回退步骤", description = "回退到上一步或指定步骤，后续步骤状态将重置")
    public Result<Void> stepBack(
            @PathVariable("id") Long registerId,
            @RequestParam(required = false) Integer targetStep) {
        enterpriseRegisterService.stepBack(registerId, targetStep);
        return Result.success("回退成功");
    }

    @PostMapping("/register/{id}/submit")
    @Operation(summary = "提交入驻申请", description = "所有校验项通过后，提交入驻申请进入待审核状态")
    public Result<Void> submitForReview(@PathVariable("id") Long registerId) {
        enterpriseRegisterService.submitForReview(registerId);
        return Result.success("入驻申请已提交，等待管理员审核");
    }
}
