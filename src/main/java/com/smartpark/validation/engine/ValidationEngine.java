package com.smartpark.validation.engine;

import com.smartpark.mapper.ValidationConfigMapper;
import com.smartpark.pojo.dto.CheckResultDTO;
import com.smartpark.pojo.entity.EnterpriseRegister;
import com.smartpark.pojo.entity.ValidationConfig;
import com.smartpark.validation.cache.DistributedValidationCache;
import com.smartpark.validation.strategy.ValidationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ValidationEngine {

    private final DistributedValidationCache cache;
    private final ValidationConfigMapper configMapper;
    private final Map<String, ValidationStrategy> strategyMap;

    public ValidationEngine(List<ValidationStrategy> strategies,
                            DistributedValidationCache cache,
                            ValidationConfigMapper configMapper) {
        this.cache = cache;
        this.configMapper = configMapper;
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        ValidationStrategy::getIdentifier,
                        Function.identity()
                ));

        validateOnStartup();
    }

    private void validateOnStartup() {
        List<ValidationConfig> allConfigs = configMapper.selectAllEnabled();
        List<String> dbStrategyIds = allConfigs.stream()
                .map(ValidationConfig::getStrategyId)
                .distinct()
                .toList();

        List<String> missingIds = dbStrategyIds.stream()
                .filter(id -> !this.strategyMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new IllegalStateException(
                    "数据库中存在未注册的策略标识符：" + missingIds);
        }

        log.info("校验引擎初始化完成，策略映射：{}", strategyMap.keySet());
    }

    public List<ValidationStrategy> loadStrategies(Long typeId, Integer configVersion) {
        List<ValidationConfig> configs = cache.getConfigs(typeId, configVersion);

        if (configs.isEmpty()) {
            configs = configMapper.selectByTypeAndVersion(typeId, configVersion);
            if (configs.isEmpty()) {
                throw new IllegalStateException("企业类型未配置校验步骤，typeId=" + typeId);
            }
            cache.setConfigs(typeId, configVersion, configs);
        }

        List<ValidationConfig> enabledConfigs = configs.stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .sorted(Comparator.comparingInt(ValidationConfig::getStepOrder))
                .toList();

        if (enabledConfigs.isEmpty()) {
            throw new IllegalStateException("该企业类型无启用的校验步骤");
        }

        for (int i = 0; i < enabledConfigs.size(); i++) {
            int expectedOrder = i + 1;
            if (enabledConfigs.get(i).getStepOrder() != expectedOrder) {
                throw new IllegalStateException(
                        String.format("步骤序号不连续，期望第%d步，实际第%d步",
                                expectedOrder, enabledConfigs.get(i).getStepOrder()));
            }
        }

        return enabledConfigs.stream()
                .map(c -> {
                    ValidationStrategy s = strategyMap.get(c.getStrategyId());
                    if (s == null) {
                        throw new IllegalStateException(
                                "策略未注册：" + c.getStrategyId());
                    }
                    return s;
                })
                .toList();
    }

    public StepExecutionResult executeStep(EnterpriseRegister register,
                                           int stepOrder,
                                           Map<String, Object> formData) {
        List<ValidationStrategy> strategies = loadStrategies(
                register.getTypeId(), register.getConfigVersion());

        if (stepOrder < 1 || stepOrder > strategies.size()) {
            throw new IllegalArgumentException(
                    String.format("步骤序号越界：%d，总步骤数：%d", stepOrder, strategies.size()));
        }

        ValidationStrategy strategy = strategies.get(stepOrder - 1);
        CheckResultDTO result = strategy.validate(formData);

        return StepExecutionResult.builder()
                .result(result)
                .strategyId(strategy.getIdentifier())
                .totalSteps(strategies.size())
                .build();
    }

    public int getTotalSteps(Long typeId, Integer configVersion) {
        return loadStrategies(typeId, configVersion).size();
    }

    public ValidationStrategy getStrategy(String strategyId) {
        return strategyMap.get(strategyId);
    }
}
