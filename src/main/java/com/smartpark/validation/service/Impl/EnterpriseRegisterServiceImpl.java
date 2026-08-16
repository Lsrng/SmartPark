package com.smartpark.validation.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartpark.mapper.EnterpriseCheckRecordMapper;
import com.smartpark.mapper.EnterpriseRegisterMapper;
import com.smartpark.pojo.dto.CheckResultDTO;
import com.smartpark.pojo.entity.EnterpriseCheckRecord;
import com.smartpark.pojo.entity.EnterpriseRegister;
import com.smartpark.pojo.vo.ProgressVO;
import com.smartpark.pojo.vo.StepInfoVO;
import com.smartpark.validation.engine.StepExecutionResult;
import com.smartpark.validation.engine.ValidationEngine;
import com.smartpark.validation.enums.CheckRecordStatus;
import com.smartpark.validation.enums.RegisterStatus;
import com.smartpark.validation.strategy.ValidationStrategy;
import com.smartpark.validation.service.ConfigVersionService;
import com.smartpark.validation.service.EnterpriseRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseRegisterServiceImpl implements EnterpriseRegisterService {

    private final EnterpriseRegisterMapper registerMapper;
    private final EnterpriseCheckRecordMapper recordMapper;
    private final ValidationEngine validationEngine;
    private final ConfigVersionService versionService;

    @Override
    @Transactional
    public EnterpriseRegister startRegister(String enterpriseName, String unifiedCode, Long typeId) {
        int currentVersion = versionService.currentVersion(typeId);

        EnterpriseRegister register = EnterpriseRegister.builder()
                .enterpriseName(enterpriseName)
                .unifiedCode(unifiedCode)
                .typeId(typeId)
                .currentStep(0)
                .configVersion(currentVersion)
                .status(RegisterStatus.CHECKING.getCode())
                .expireAt(LocalDateTime.now().plusDays(30))
                .version(0)
                .build();

        registerMapper.insert(register);
        log.info("创建入驻申请：registerId={}, typeId={}, configVersion={}, expireAt={}",
                register.getId(), typeId, currentVersion, register.getExpireAt());
        return register;
    }

    @Override
    @Transactional
    public ProgressVO submitStep(Long registerId, Integer stepOrder,
                                  Map<String, Object> formData, Long operatorId) {
        EnterpriseRegister register = getById(registerId);
        checkExpired(register);

        StepExecutionResult execResult = validationEngine.executeStep(
                register, stepOrder, formData);

        CheckResultDTO result = execResult.getResult();

        EnterpriseCheckRecord record = EnterpriseCheckRecord.builder()
                .registerId(registerId)
                .stepOrder(stepOrder)
                .strategyId(execResult.getStrategyId())
                .status(result.getPassed()
                        ? CheckRecordStatus.PASSED.getCode()
                        : CheckRecordStatus.FAILED.getCode())
                .formData(formData)
                .checkResult(result.getDetail())
                .errorCode(result.getErrorCode())
                .errorMessage(result.getErrorMessage())
                .operatorId(operatorId)
                .build();
        recordMapper.insert(record);

        if (result.getPassed()) {
            int totalSteps = execResult.getTotalSteps();
            int nextStep = stepOrder;

            if (stepOrder >= totalSteps) {
                register.setCurrentStep(stepOrder);
                register.setStatus(RegisterStatus.ALL_CHECKED.getCode());
            } else {
                nextStep = stepOrder + 1;
                register.setCurrentStep(stepOrder);
            }
            register.setVersion(register.getVersion() + 1);
            registerMapper.updateById(register);

            return getProgress(registerId);
        } else {
            return getProgress(registerId);
        }
    }

    @Override
    @Transactional
    public ProgressVO backStep(Long registerId, Integer stepOrder, Long operatorId) {
        EnterpriseRegister register = getById(registerId);
        checkExpired(register);

        recordMapper.resetStepsAfter(registerId, stepOrder);

        register.setCurrentStep(stepOrder - 1);
        register.setStatus(RegisterStatus.CHECKING.getCode());
        register.setVersion(register.getVersion() + 1);
        registerMapper.updateById(register);

        log.info("步骤回退：registerId={}, targetStep={}", registerId, stepOrder);
        return getProgress(registerId);
    }

    @Override
    public ProgressVO getProgress(Long registerId) {
        EnterpriseRegister register = getById(registerId);

        List<EnterpriseCheckRecord> records = recordMapper.selectByRegisterId(registerId);
        Map<Integer, EnterpriseCheckRecord> recordMap = records.stream()
                .collect(Collectors.toMap(
                        EnterpriseCheckRecord::getStepOrder, r -> r, (a, b) -> a));

        List<ValidationStrategy> strategies = validationEngine.loadStrategies(
                register.getTypeId(), register.getConfigVersion());

        List<StepInfoVO> steps = new ArrayList<>();
        for (int i = 0; i < strategies.size(); i++) {
            int stepOrder = i + 1;
            EnterpriseCheckRecord record = recordMap.get(stepOrder);
            String status = record != null
                    ? record.getStatus()
                    : CheckRecordStatus.PENDING.getCode();

            ValidationStrategy strategy = strategies.get(i);
            steps.add(StepInfoVO.builder()
                    .stepOrder(stepOrder)
                    .strategyId(strategy.getIdentifier())
                    .strategyName(strategy.getDisplayName())
                    .status(status)
                    .enabled(true)
                    .build());
        }

        return ProgressVO.builder()
                .registerId(registerId)
                .currentStep(register.getCurrentStep())
                .status(register.getStatus())
                .steps(steps)
                .build();
    }

    @Override
    public EnterpriseRegister getById(Long registerId) {
        EnterpriseRegister register = registerMapper.selectById(registerId);
        if (register == null) {
            throw new IllegalArgumentException("入驻申请不存在：" + registerId);
        }
        return register;
    }

    /**
     * 校验入驻申请是否已过期
     * 如果申请已过期，抛出异常阻止继续操作
     */
    private void checkExpired(EnterpriseRegister register) {
        if (RegisterStatus.EXPIRED.getCode().equals(register.getStatus())) {
            throw new IllegalArgumentException("您的入驻申请已过期，请重新发起入驻申请");
        }
    }

    @Override
    @Transactional
    public void rollbackTo(Long registerId, int targetStep, Long operatorId) {
        EnterpriseRegister register = getById(registerId);
        checkExpired(register);

        if (targetStep < 0 || targetStep > register.getCurrentStep()) {
            throw new IllegalArgumentException(
                    String.format("回滚目标步骤非法：%d, 当前步骤：%d",
                            targetStep, register.getCurrentStep()));
        }

        recordMapper.resetStepsAfter(registerId, targetStep);
        register.setCurrentStep(targetStep);
        register.setStatus(targetStep == 0
                ? RegisterStatus.DRAFT.getCode()
                : RegisterStatus.CHECKING.getCode());
        register.setVersion(register.getVersion() + 1);
        registerMapper.updateById(register);

        log.info("入驻回滚：registerId={}, targetStep={}", registerId, targetStep);
    }
}
