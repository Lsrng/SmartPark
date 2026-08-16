package com.smartpark.validation.service;

import com.smartpark.pojo.entity.EnterpriseRegister;
import com.smartpark.pojo.vo.ProgressVO;

import java.util.Map;

public interface EnterpriseRegisterService {

    EnterpriseRegister startRegister(String enterpriseName, String unifiedCode, Long typeId);

    ProgressVO submitStep(Long registerId, Integer stepOrder, Map<String, Object> formData, Long operatorId);

    ProgressVO backStep(Long registerId, Integer stepOrder, Long operatorId);

    ProgressVO getProgress(Long registerId);

    EnterpriseRegister getById(Long registerId);

    void rollbackTo(Long registerId, int targetStep, Long operatorId);
}
