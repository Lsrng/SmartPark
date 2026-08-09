package com.smartpark.monitor.sender;

/**
 * 短信发送抽象
 * <p>
 * 实现类自带失败重试（如重试 2 次，指数退避）。
 * 后续接阿里云/腾讯云短信时新增实现类 + 条件注解切换即可，业务零改动。
 * </p>
 */
public interface SmsSender {

    /**
     * 发送短信
     *
     * @param phone   收件人手机号
     * @param content 短信内容
     */
    void send(String phone, String content);
}
