package com.smartpark.validation.strategy.impl;

import com.smartpark.pojo.dto.CheckResultDTO;
import com.smartpark.validation.strategy.ValidationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class BlacklistStrategy implements ValidationStrategy {

    @Override
    public String getIdentifier() {
        return "BLACKLIST";
    }

    @Override
    public String getDisplayName() {
        return "黑名单校验";
    }

    @Override
    public CheckResultDTO validate(Map<String, Object> formData) {
        String unifiedCode = (String) formData.get("unifiedCode");

        if (unifiedCode == null || unifiedCode.trim().isEmpty()) {
            return CheckResultDTO.fail("BLACKLIST_CODE_EMPTY", "统一社会信用代码不能为空");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("unifiedCode", unifiedCode);
        detail.put("inBlacklist", false);
        return CheckResultDTO.pass(detail);
    }
}
