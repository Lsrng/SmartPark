package com.smartpark.enterprise.chain;

import com.smartpark.common.exception.EnterpriseCheckException;
import com.smartpark.enterprise.handler.CheckHandler;
import com.smartpark.mapper.CheckItemDefMapper;
import com.smartpark.mapper.EnterpriseTypeCheckMapper;
import com.smartpark.pojo.entity.enterprise.CheckItemDef;
import com.smartpark.pojo.entity.enterprise.EnterpriseTypeCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 链构造器 — 从数据库配置动态构建校验链
 * <p>
 * 1. 从 enterprise_type_check 查出某类型的所有启用的校验步骤
 * 2. 关联 check_item_def 获取 handler_bean 名称
 * 3. 从 Spring 容器获取对应的 Handler Bean
 * 4. 组装成 CheckChain 返回
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainBuilder {

    private final EnterpriseTypeCheckMapper enterpriseTypeCheckMapper;
    private final CheckItemDefMapper checkItemDefMapper;
    private final ApplicationContext applicationContext;

    /**
     * 根据企业类型ID构建校验链
     *
     * @param typeId 企业类型ID
     * @return 校验链
     * @throws EnterpriseCheckException 当配置不存在或不完整时抛出
     */
    public CheckChain build(Long typeId) {
        // 1. 查询启用的校验步骤（按 step_order 排序）
        List<EnterpriseTypeCheck> typeChecks = enterpriseTypeCheckMapper.selectEnabledByTypeId(typeId);

        if (typeChecks == null || typeChecks.isEmpty()) {
            throw new EnterpriseCheckException("企业类型未配置校验步骤，typeId=" + typeId);
        }

        // 2. 组装节点和元信息
        List<CheckHandler> nodes = new ArrayList<>(typeChecks.size());
        List<StepInfo> stepInfos = new ArrayList<>(typeChecks.size());

        for (EnterpriseTypeCheck typeCheck : typeChecks) {
            // 查询校验项定义
            CheckItemDef itemDef = checkItemDefMapper.selectById(typeCheck.getCheckItemId());
            if (itemDef == null || !"ENABLED".equals(itemDef.getStatus())) {
                throw new EnterpriseCheckException(
                        "校验项配置不存在或已禁用，checkItemId=" + typeCheck.getCheckItemId());
            }

            // 从 Spring 容器获取 Handler Bean
            CheckHandler handler = applicationContext.getBean(itemDef.getHandlerBean(), CheckHandler.class);
            if (handler == null) {
                throw new EnterpriseCheckException(
                        "校验处理器未找到，handlerBean=" + itemDef.getHandlerBean());
            }

            nodes.add(handler);
            stepInfos.add(new StepInfo(
                    typeCheck.getStepOrder(),
                    itemDef.getName(),
                    itemDef.getCode(),
                    itemDef.getId()));
        }

        log.info("构建校验链完成 - typeId: {}, steps: {}", typeId, stepInfos.size());
        return new CheckChain(nodes, stepInfos);
    }
}
