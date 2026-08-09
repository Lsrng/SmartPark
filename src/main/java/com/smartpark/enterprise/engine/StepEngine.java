package com.smartpark.enterprise.engine;

import com.alibaba.fastjson2.JSON;
import com.smartpark.common.enumeration.CheckStatus;
import com.smartpark.common.enumeration.RegisterStatus;
import com.smartpark.common.exception.EnterpriseCheckException;
import com.smartpark.enterprise.chain.ChainBuilder;
import com.smartpark.enterprise.chain.CheckChain;
import com.smartpark.enterprise.handler.CheckHandler;
import com.smartpark.enterprise.handler.CheckRequest;
import com.smartpark.enterprise.handler.CheckResult;
import com.smartpark.mapper.EnterpriseCheckRecordMapper;
import com.smartpark.mapper.EnterpriseRegisterMapper;
import com.smartpark.pojo.entity.enterprise.EnterpriseCheckRecord;
import com.smartpark.pojo.entity.enterprise.EnterpriseRegister;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 步骤引擎 — 控制校验链的执行进度
 * <p>
 * 核心职责：
 * 1. 校验前置步骤是否已通过
 * 2. 从链中获取当前节点并执行校验
 * 3. 保存校验记录
 * 4. 推进或回退进度
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StepEngine {

    private final EnterpriseRegisterMapper registerMapper;
    private final EnterpriseCheckRecordMapper checkRecordMapper;
    private final ChainBuilder chainBuilder;

    /**
     * 执行指定步骤的校验
     * <p>
     * 流程：
     * 1. 校验该类型是否有校验链配置
     * 2. 校验前置步骤是否已通过
     * 3. 获取当前节点的 Handler 并执行校验
     * 4. 保存校验记录
     * 5. 若通过则自动推进到下一步
     *
     * @param register  入驻申请
     * @param stepOrder 当前执行的步骤序号
     * @param formData  提交的表单数据
     * @param operatorId 操作人ID
     * @return 校验结果
     */
    @Transactional(rollbackFor = Exception.class)
    public CheckResult executeStep(EnterpriseRegister register, Integer stepOrder,
                                    Map<String, Object> formData, Long operatorId) {
        // 1. 构建校验链
        CheckChain chain = chainBuilder.build(register.getTypeId());

        // 2. 校验步骤序号合法性
        if (stepOrder > chain.getTotalSteps()) {
            throw new EnterpriseCheckException("步骤序号超出总步骤数");
        }

        // 3. 校验前置步骤是否已通过
        validatePreviousStepPassed(register.getId(), stepOrder);

        // 4. 获取当前节点并执行校验
        CheckHandler handler = chain.getNode(stepOrder);
        CheckRequest request = CheckRequest.builder()
                .register(register)
                .stepOrder(stepOrder)
                .formData(formData)
                .operatorId(operatorId)
                .build();

        CheckResult result = handler.check(request);

        // 5. 保存校验记录
        EnterpriseCheckRecord record = checkRecordMapper.selectByRegisterIdAndStep(register.getId(), stepOrder);
        if (record == null) {
            record = new EnterpriseCheckRecord();
            record.setRegisterId(register.getId());
            record.setCheckItemId(chain.getStepInfo(stepOrder).getCheckItemId());
            record.setStepOrder(stepOrder);
        }

        record.setCheckStatus(result.isPassed() ? CheckStatus.PASSED.getCode() : CheckStatus.FAILED.getCode());
        record.setCheckResult(JSON.toJSONString(result));
        record.setCheckedBy(operatorId);
        record.setCheckedAt(LocalDateTime.now());

        if (record.getId() == null) {
            checkRecordMapper.insert(record);
        } else {
            checkRecordMapper.updateById(record);
        }

        // 6. 更新入驻申请状态
        if (result.isPassed()) {
            if (stepOrder < chain.getTotalSteps()) {
                // 非最后一步，推进到下一步
                register.setCurrentStep(stepOrder + 1);
                register.setStatus(RegisterStatus.CHECKING.getCode());
            } else {
                // 最后一步通过，状态变为全部通过
                register.setCurrentStep(stepOrder);
                register.setStatus(RegisterStatus.ALL_CHECKED.getCode());
            }
        } else {
            // 校验失败，停留在当前步骤
            register.setCurrentStep(stepOrder);
            register.setStatus(RegisterStatus.CHECKING.getCode());
        }

        registerMapper.updateById(register);

        log.info("步骤校验完成 - registerId: {}, step: {}, passed: {}",
                register.getId(), stepOrder, result.isPassed());

        return result;
    }

    /**
     * 校验前置步骤是否已全部通过
     */
    private void validatePreviousStepPassed(Long registerId, int stepOrder) {
        if (stepOrder <= 1) {
            return;
        }
        for (int i = 1; i < stepOrder; i++) {
            EnterpriseCheckRecord record = checkRecordMapper.selectPassedStep(registerId, i);
            if (record == null) {
                throw new EnterpriseCheckException(
                        String.format("前置步骤（第%d步）尚未通过，请先完成前置校验", i));
            }
        }
    }

    /**
     * 回退到指定步骤
     * <p>
     * 1. 校验目标步骤是否小于当前步骤（不能向前回退）
     * 2. 更新入驻申请的 current_step
     * 3. 重置目标步骤之后的所有校验记录为 PENDING
     *
     * @param register   入驻申请
     * @param targetStep 目标步骤序号
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollback(EnterpriseRegister register, int targetStep) {
        if (targetStep >= register.getCurrentStep()) {
            throw new EnterpriseCheckException("目标步骤必须小于当前步骤");
        }

        // 重置目标步骤之后的校验记录
        checkRecordMapper.resetFromStep(register.getId(), targetStep + 1);

        // 更新当前步骤
        register.setCurrentStep(targetStep);
        register.setStatus(RegisterStatus.CHECKING.getCode());
        registerMapper.updateById(register);

        log.info("回退步骤完成 - registerId: {}, from step: {} to step: {}",
                register.getId(), register.getCurrentStep(), targetStep);
    }

    /**
     * 获取某类型入驻申请的校验链
     */
    public CheckChain getCheckChain(Long typeId) {
        return chainBuilder.build(typeId);
    }
}
