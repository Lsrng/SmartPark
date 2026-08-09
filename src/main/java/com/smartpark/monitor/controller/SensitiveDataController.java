package com.smartpark.monitor.controller;

import com.smartpark.common.result.Result;
import com.smartpark.monitor.annotation.FrequencyMonitor;
import com.smartpark.monitor.vo.UserSensitiveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 敏感数据查询演示接口（旁路监控标注示例）
 * <p>
 * 监控维度组合：
 * <ul>
 *   <li><b>账号维度</b>：20 次/分钟（正常峰值 ×3~5，抓内部账号越权/被盗用批量查询）</li>
 *   <li><b>全局水位</b>：500 次/分钟，防多账号并发批量拉取绕过单账号水位</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/sensitive")
@Tag(name = "敏感数据查询（旁路监控演示）")
@Slf4j
public class SensitiveDataController {

    @GetMapping("/user/{id}")
    @Operation(summary = "查询用户敏感信息")
    @FrequencyMonitor(prefix = "monitor:user-query",
            keyField = "#userId", limit = 20, windowSeconds = 60,       // 单账号：20 次/分钟
            globalAlert = true,                                          // 接口全局水位兜底
            globalLimit = 500, globalWindowSeconds = 60)                 // 全局：500 次/分钟
    public Result<UserSensitiveVO> queryUser(@PathVariable Long id) {
        // 业务：鉴权 → 查询用户敏感信息（手机号、证件号等，按权限脱敏）
        UserSensitiveVO vo = new UserSensitiveVO();
        vo.setUserId(id);
        vo.setUsername("user_" + id);
        vo.setPhone("138****8000");
        vo.setIdCard("1101**********1234");
        log.info("查询用户敏感信息 id={}", id);
        return Result.success(vo);
    }
}
