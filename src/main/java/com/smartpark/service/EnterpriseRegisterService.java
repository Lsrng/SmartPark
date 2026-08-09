package com.smartpark.service;

import com.smartpark.enterprise.handler.CheckResult;
import com.smartpark.pojo.dto.enterprise.StartRegisterRequest;
import com.smartpark.pojo.dto.enterprise.StepSaveRequest;
import com.smartpark.pojo.dto.enterprise.StepSubmitRequest;
import com.smartpark.pojo.vo.enterprise.EnterpriseTypeVO;
import com.smartpark.pojo.vo.enterprise.ProgressVO;
import com.smartpark.pojo.vo.enterprise.StepInfoVO;

import java.util.List;
import java.util.Map;

public interface EnterpriseRegisterService {

    /**
     * 获取所有启用的企业类型
     */
    List<EnterpriseTypeVO> getEnterpriseTypes();

    /**
     * 开始入驻，创建入驻申请草稿
     */
    Long startRegister(StartRegisterRequest request, Long userId);

    /**
     * 获取入驻进度
     */
    ProgressVO getProgress(Long registerId);

    /**
     * 保存当前步骤草稿数据
     */
    void saveStepDraft(StepSaveRequest request);

    /**
     * 获取某步骤已保存的草稿数据
     */
    Map<String, Object> getStepDraft(Long registerId, Integer stepOrder);

    /**
     * 获取入驻申请的所有校验步骤信息
     */
    List<StepInfoVO> getSteps(Long registerId);

    /**
     * 提交当前步骤校验
     */
    CheckResult submitStep(StepSubmitRequest request, Long userId);

    /**
     * 回退到上一步或指定步骤
     */
    void stepBack(Long registerId, Integer targetStep);

    /**
     * 所有校验通过后，提交入驻申请
     */
    void submitForReview(Long registerId);
}
