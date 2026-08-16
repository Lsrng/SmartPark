package com.smartpark.validation.strategy;

import com.smartpark.pojo.dto.CheckResultDTO;

import java.util.Map;

public interface ValidationStrategy {

    String getIdentifier();

    default String getDisplayName() {
        return getIdentifier();
    }

    CheckResultDTO validate(Map<String, Object> formData);
}
