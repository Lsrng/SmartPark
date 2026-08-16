package com.smartpark.validation.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartpark.mapper.EnterpriseRegisterMapper;
import com.smartpark.pojo.entity.EnterpriseRegister;
import com.smartpark.validation.enums.RegisterStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterCleanupTask {

    private final EnterpriseRegisterMapper registerMapper;

    /**
     * 每小时执行一次，清理过期的入驻申请
     * 仅处理「草稿/校验中/校验完成」状态的申请，排除审核中、终态申请
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanExpiredRegisters() {
        LocalDateTime now = LocalDateTime.now();

        List<EnterpriseRegister> expiredList = registerMapper.selectList(
                new QueryWrapper<EnterpriseRegister>()
                        .lt("expire_at", now)
                        .in("status",
                                RegisterStatus.DRAFT.getCode(),
                                RegisterStatus.CHECKING.getCode(),
                                RegisterStatus.ALL_CHECKED.getCode()
                        )
        );

        if (expiredList.isEmpty()) {
            return;
        }

        int count = 0;
        for (EnterpriseRegister register : expiredList) {
            register.setStatus(RegisterStatus.EXPIRED.getCode());
            registerMapper.updateById(register);
            count++;
        }

        log.info("清理过期入驻申请：共 {} 条，状态已更新为 EXPIRED", count);
    }
}
