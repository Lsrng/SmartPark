package com.smartpark.enterprise.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 金融许可证校验处理器
 * <p>
 * 校验金融许可证有效性。
 * 当前为模拟实现，实际可对接金融监管机构API或人工审核。
 * </p>
 */
@Slf4j
@Component("financialLicenseHandler")
public class FinancialLicenseHandler implements CheckHandler {

    @Override
    public CheckResult check(CheckRequest request) {
        log.info("金融许可证校验开始 - 企业: {}",
                request.getRegister().getEnterpriseName());

        // 从表单数据获取金融许可证信息
        String licenseNo = null;
        if (request.getFormData() != null) {
            Object no = request.getFormData().get("financialLicenseNo");
            if (no != null) {
                licenseNo = no.toString();
            }
        }

        // 1. 基本非空校验
        if (licenseNo == null || licenseNo.isBlank()) {
            return CheckResult.failed("金融许可证编号不能为空");
        }

        // 2. 格式校验（模拟：以 FL 开头 + 8位数字）
        if (!licenseNo.matches("FL\\d{8}")) {
            return CheckResult.failed("金融许可证编号格式不正确，应为 FL+8位数字（如 FL20240001）");
        }

        // 3. 模拟校验通过
        return CheckResult.success("金融许可证校验通过，许可证编号有效");
    }
}
