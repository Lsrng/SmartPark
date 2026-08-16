package com.smartpark.validation.engine;

import com.smartpark.pojo.dto.CheckResultDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionResult {

    private CheckResultDTO result;
    private String strategyId;
    private int totalSteps;
}
