package com.smartpark.enterprise.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 法人信息校验处理器
 * <p>
 * 校验法定代表人信息真实性：姓名、联系电话等。
 * 当前为模拟实现，实际可对接公安接口或人工审核。
 * </p>
 */
@Slf4j
@Component("legalPersonHandler")
public class LegalPersonHandler implements CheckHandler {

    @Override
    public CheckResult check(CheckRequest request) {
        log.info("法人信息校验开始 - 企业: {}, 法人: {}",
                request.getRegister().getEnterpriseName(),
                request.getRegister().getLegalPerson());

        String legalPerson = request.getRegister().getLegalPerson();
        String legalPersonPhone = request.getRegister().getLegalPersonPhone();

        // 1. 法人姓名校验
        if (legalPerson == null || legalPerson.isBlank()) {
            return CheckResult.failed("法定代表人姓名不能为空");
        }
        if (legalPerson.length() < 2 || legalPerson.length() > 50) {
            return CheckResult.failed("法定代表人姓名长度不合法");
        }

        // 2. 法人联系电话校验
        if (legalPersonPhone == null || legalPersonPhone.isBlank()) {
            return CheckResult.failed("法人联系电话不能为空");
        }
        if (!legalPersonPhone.matches("1[3-9]\\d{9}")) {
            return CheckResult.failed("法人联系电话格式不正确，应为11位手机号");
        }

        // 3. 模拟校验通过
        return CheckResult.success("法人信息校验通过，法定代表人信息真实有效");
    }
}
