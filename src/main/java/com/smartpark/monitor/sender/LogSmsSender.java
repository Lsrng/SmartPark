package com.smartpark.monitor.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志模拟短信发送（先跑通全链路，不真正调用短信网关）
 */
@Component
@Slf4j
public class LogSmsSender implements SmsSender {

    @Override
    public void send(String phone, String content) {
        log.warn("[SMS模拟] to={}, content={}", phone, content);
    }
}
