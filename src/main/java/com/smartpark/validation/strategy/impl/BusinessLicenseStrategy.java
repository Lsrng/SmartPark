package com.smartpark.validation.strategy.impl;

import com.smartpark.pojo.dto.CheckResultDTO;
import com.smartpark.validation.strategy.ValidationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class BusinessLicenseStrategy implements ValidationStrategy {

    @Override
    public String getIdentifier() {
        return "BUSINESS_LICENSE";
    }

    @Override
    public String getDisplayName() {
        return "营业执照校验";
    }

    @Override
    public CheckResultDTO validate(Map<String, Object> formData) {
        String licenseNo = (String) formData.get("licenseNo");
        if (licenseNo == null || licenseNo.trim().isEmpty()) {
            return CheckResultDTO.fail("LICENSE_EMPTY", "营业执照号不能为空");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("licenseNo", licenseNo);
        detail.put("verified", true);
        return CheckResultDTO.pass(detail);
    }
}
