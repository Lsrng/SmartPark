package com.smartpark.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.smartpark.common.enumeration.CheckStatus;
import com.smartpark.common.enumeration.RegisterStatus;
import com.smartpark.common.exception.EnterpriseCheckException;
import com.smartpark.enterprise.chain.CheckChain;
import com.smartpark.enterprise.chain.StepInfo;
import com.smartpark.enterprise.engine.StepEngine;
import com.smartpark.enterprise.handler.CheckResult;
import com.smartpark.mapper.EnterpriseCheckRecordMapper;
import com.smartpark.mapper.EnterpriseRegisterMapper;
import com.smartpark.mapper.EnterpriseTypeCheckMapper;
import com.smartpark.mapper.EnterpriseTypeMapper;
import com.smartpark.pojo.dto.enterprise.StartRegisterRequest;
import com.smartpark.pojo.dto.enterprise.StepSaveRequest;
import com.smartpark.pojo.dto.enterprise.StepSubmitRequest;
import com.smartpark.pojo.entity.enterprise.EnterpriseCheckRecord;
import com.smartpark.pojo.entity.enterprise.EnterpriseRegister;
import com.smartpark.pojo.entity.enterprise.EnterpriseType;
import com.smartpark.pojo.vo.enterprise.EnterpriseTypeVO;
import com.smartpark.pojo.vo.enterprise.ProgressVO;
import com.smartpark.pojo.vo.enterprise.StepInfoVO;
import com.smartpark.service.EnterpriseRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseRegisterServiceImpl implements EnterpriseRegisterService {

    private final EnterpriseTypeMapper enterpriseTypeMapper;
    private final EnterpriseRegisterMapper registerMapper;
    private final EnterpriseCheckRecordMapper checkRecordMapper;
    private final EnterpriseTypeCheckMapper typeCheckMapper;
    private final StepEngine stepEngine;

    @Override
    public List<EnterpriseTypeVO> getEnterpriseTypes() {
        List<EnterpriseType> types = enterpriseTypeMapper.selectList(null);
        return types.stream()
                .filter(t -> "ENABLED".equals(t.getStatus()))
                .map(t -> {
                    Integer steps = typeCheckMapper.countStepsByTypeId(t.getId());
                    return new EnterpriseTypeVO(t.getId(), t.getName(), t.getCode(),
                            t.getDescription(), steps != null ? steps : 0);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startRegister(StartRegisterRequest request, Long userId) {
        // 校验企业类型
        EnterpriseType type = enterpriseTypeMapper.selectById(request.getTypeId());
        if (type == null || !"ENABLED".equals(type.getStatus())) {
            throw new EnterpriseCheckException("企业类型不存在或已禁用");
        }

        // 创建入驻申请
        EnterpriseRegister register = new EnterpriseRegister();
        register.setEnterpriseName(request.getEnterpriseName());
        register.setTypeId(request.getTypeId());
        register.setUnifiedCode(request.getUnifiedCode());
        register.setLegalPerson(request.getLegalPerson());
        register.setLegalPersonPhone(request.getLegalPersonPhone());
        register.setContactName(request.getContactName());
        register.setContactPhone(request.getContactPhone());
        register.setContactEmail(request.getContactEmail());
        register.setAddress(request.getAddress());
        register.setCurrentStep(1);
        register.setStatus(RegisterStatus.DRAFT.getCode());
        register.setCreatedBy(userId);
        register.setDraftData("{}");

        registerMapper.insert(register);
        log.info("创建入驻申请 - id: {}, enterprise: {}, type: {}",
                register.getId(), request.getEnterpriseName(), type.getName());

        // 自动推进到 CHECKING 状态，进入第1步
        register.setStatus(RegisterStatus.CHECKING.getCode());
        registerMapper.updateById(register);

        return register.getId();
    }

    @Override
    public ProgressVO getProgress(Long registerId) {
        EnterpriseRegister register = registerMapper.selectById(registerId);
        if (register == null) {
            throw new EnterpriseCheckException("入驻申请不存在");
        }

        CheckChain chain = stepEngine.getCheckChain(register.getTypeId());
        List<EnterpriseCheckRecord> records = checkRecordMapper.selectByRegisterId(registerId);

        // 构建步骤信息
        List<StepInfoVO> steps = new ArrayList<>(chain.getTotalSteps());
        for (int i = 1; i <= chain.getTotalSteps(); i++) {
            StepInfo stepInfo = chain.getStepInfo(i);
            EnterpriseCheckRecord record = findRecord(records, i);

            String checkStatus = CheckStatus.PENDING.getCode();
            String checkResult = null;
            if (record != null) {
                checkStatus = record.getCheckStatus();
                checkResult = record.getCheckResult();
            }

            boolean isCurrent = register.getCurrentStep().equals(i);
            boolean unlocked = isStepUnlocked(records, register, i);

            steps.add(StepInfoVO.builder()
                    .stepOrder(i)
                    .stepName(stepInfo.getStepName())
                    .stepCode(stepInfo.getStepCode())
                    .checkStatus(checkStatus)
                    .checkResult(checkResult)
                    .isCurrent(isCurrent)
                    .unlocked(unlocked)
                    .build());
        }

        return ProgressVO.builder()
                .registerId(register.getId())
                .enterpriseName(register.getEnterpriseName())
                .status(register.getStatus())
                .currentStep(register.getCurrentStep())
                .totalSteps(chain.getTotalSteps())
                .steps(steps)
                .build();
    }

    @Override
    public List<StepInfoVO> getSteps(Long registerId) {
        ProgressVO progress = getProgress(registerId);
        return progress.getSteps();
    }

    @Override
    public void saveStepDraft(StepSaveRequest request) {
        EnterpriseRegister register = registerMapper.selectById(request.getRegisterId());
        if (register == null) {
            throw new EnterpriseCheckException("入驻申请不存在");
        }

        // 解析已有草稿
        JSONObject draft = JSONObject.parseObject(register.getDraftData() != null
                ? register.getDraftData() : "{}");

        // 保存当前步骤的草稿
        JSONObject stepData = new JSONObject(request.getFormData());
        draft.put("step_" + request.getStepOrder(), stepData);
        register.setDraftData(JSON.toJSONString(draft));
        registerMapper.updateById(register);

        log.info("保存草稿 - registerId: {}, step: {}", request.getRegisterId(), request.getStepOrder());
    }

    @Override
    public Map<String, Object> getStepDraft(Long registerId, Integer stepOrder) {
        EnterpriseRegister register = registerMapper.selectById(registerId);
        if (register == null) {
            throw new EnterpriseCheckException("入驻申请不存在");
        }

        if (register.getDraftData() == null || "{}".equals(register.getDraftData())) {
            return null;
        }

        JSONObject draft = JSONObject.parseObject(register.getDraftData());
        JSONObject stepData = draft.getJSONObject("step_" + stepOrder);
        return stepData != null ? stepData : null;
    }

    @Override
    public CheckResult submitStep(StepSubmitRequest request, Long userId) {
        EnterpriseRegister register = registerMapper.selectById(request.getRegisterId());
        if (register == null) {
            throw new EnterpriseCheckException("入驻申请不存在");
        }

        // 校验是否处于可校验状态
        if (RegisterStatus.APPROVED.getCode().equals(register.getStatus())
                || RegisterStatus.REJECTED.getCode().equals(register.getStatus())) {
            throw new EnterpriseCheckException("入驻申请已结束，无法继续校验");
        }

        return stepEngine.executeStep(register, request.getStepOrder(), request.getFormData(), userId);
    }

    @Override
    public void stepBack(Long registerId, Integer targetStep) {
        EnterpriseRegister register = registerMapper.selectById(registerId);
        if (register == null) {
            throw new EnterpriseCheckException("入驻申请不存在");
        }

        int target = targetStep != null ? targetStep : register.getCurrentStep() - 1;
        if (target < 1) {
            throw new EnterpriseCheckException("已经是第一步，无法回退");
        }

        stepEngine.rollback(register, target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForReview(Long registerId) {
        EnterpriseRegister register = registerMapper.selectById(registerId);
        if (register == null) {
            throw new EnterpriseCheckException("入驻申请不存在");
        }

        if (!RegisterStatus.ALL_CHECKED.getCode().equals(register.getStatus())) {
            throw new EnterpriseCheckException("所有校验项未全部通过，无法提交审核");
        }

        register.setStatus(RegisterStatus.PENDING_REVIEW.getCode());
        registerMapper.updateById(register);

        log.info("提交入驻申请审核 - registerId: {}, enterprise: {}",
                registerId, register.getEnterpriseName());
    }

    // ==================== 私有方法 ====================

    private EnterpriseCheckRecord findRecord(List<EnterpriseCheckRecord> records, int stepOrder) {
        return records.stream()
                .filter(r -> r.getStepOrder().equals(stepOrder))
                .findFirst()
                .orElse(null);
    }

    private boolean isStepUnlocked(List<EnterpriseCheckRecord> records,
                                    EnterpriseRegister register, int stepOrder) {
        if (stepOrder == 1) {
            return true;
        }
        // 检查前置步骤是否全部通过
        for (int i = 1; i < stepOrder; i++) {
            EnterpriseCheckRecord record = findRecord(records, i);
            if (record == null || !CheckStatus.PASSED.getCode().equals(record.getCheckStatus())) {
                return false;
            }
        }
        return true;
    }
}
