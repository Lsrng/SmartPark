package com.smartpark.enterprise.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 营业执照校验处理器
 * <p>
 * 校验营业执照有效性：统一社会信用代码格式校验、工商信息核验等。
 * 当前为模拟实现，实际可对接工商局API或人工审核。
 * </p>
 */
@Slf4j
@Component("businessLicenseHandler")
public class BusinessLicenseHandler implements CheckHandler {

    @Override
    public CheckResult check(CheckRequest request) {
        log.info("营业执照校验开始 - 企业: {}, 统一代码: {}",
                request.getRegister().getEnterpriseName(),
                request.getRegister().getUnifiedCode());

        String unifiedCode = request.getRegister().getUnifiedCode();

        // 1. 基本非空校验
        if (unifiedCode == null || unifiedCode.isBlank()) {
            return CheckResult.failed("统一社会信用代码不能为空");
        }

        // 2. 格式校验（18位，字母数字）
        if (!unifiedCode.matches("[A-Za-z0-9]{18}")) {
            return CheckResult.failed("统一社会信用代码格式不正确，应为18位字母数字组合");
        }

        // 3. 模拟校验通过
        return CheckResult.success("营业执照校验通过，统一社会信用代码格式正确");
    }
}
