package com.smartpark.validation.strategy.impl;

import com.smartpark.pojo.dto.CheckResultDTO;
import com.smartpark.validation.strategy.ValidationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class LegalPersonStrategy implements ValidationStrategy {

    @Override
    public String getIdentifier() {
        return "LEGAL_PERSON";
    }

    @Override
    public String getDisplayName() {
        return "法人信息校验";
    }

    @Override
    public CheckResultDTO validate(Map<String, Object> formData) {
        String legalName = (String) formData.get("legalName");
        String legalIdCard = (String) formData.get("legalIdCard");

        if (legalName == null || legalName.trim().isEmpty()) {
            return CheckResultDTO.fail("LEGAL_NAME_EMPTY", "法人姓名不能为空");
        }
        if (legalIdCard == null || legalIdCard.trim().isEmpty()) {
            return CheckResultDTO.fail("LEGAL_IDCARD_EMPTY", "法人身份证号不能为空");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("legalName", legalName);
        detail.put("verified", true);
        return CheckResultDTO.pass(detail);
    }
}
