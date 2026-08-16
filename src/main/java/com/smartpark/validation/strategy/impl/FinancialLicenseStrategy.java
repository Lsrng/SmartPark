package com.smartpark.validation.strategy.impl;

import com.smartpark.pojo.dto.CheckResultDTO;
import com.smartpark.validation.strategy.ValidationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class FinancialLicenseStrategy implements ValidationStrategy {

    @Override
    public String getIdentifier() {
        return "FINANCIAL_LICENSE";
    }

    @Override
    public String getDisplayName() {
        return "金融许可证校验";
    }

    @Override
    public CheckResultDTO validate(Map<String, Object> formData) {
        String licenseNo = (String) formData.get("financialLicenseNo");
        if (licenseNo == null || licenseNo.trim().isEmpty()) {
            return CheckResultDTO.fail("FIN_LICENSE_EMPTY", "金融许可证号不能为空");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("financialLicenseNo", licenseNo);
        detail.put("verified", true);
        return CheckResultDTO.pass(detail);
    }
}
