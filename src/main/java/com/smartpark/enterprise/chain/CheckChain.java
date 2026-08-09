package com.smartpark.enterprise.chain;

import com.smartpark.enterprise.handler.CheckHandler;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 校验链 — 由 ChainBuilder 从数据库配置动态构建
 * <p>
 * 以 List 组织有序的责任链节点，不持 next 引用。
 * 由 StepEngine 控制当前执行到哪个节点。
 * </p>
 */
@Getter
public class CheckChain {

    /** 有序的责任链节点（索引=stepOrder-1） */
    private final List<CheckHandler> nodes;

    /** 步骤元信息列表 */
    private final List<StepInfo> stepInfos;

    public CheckChain(List<CheckHandler> nodes, List<StepInfo> stepInfos) {
        this.nodes = Collections.unmodifiableList(nodes);
        this.stepInfos = Collections.unmodifiableList(stepInfos);
    }

    /**
     * 根据步骤序号获取对应的校验处理器
     *
     * @param stepOrder 步骤序号（从1开始）
     * @return 校验处理器
     * @throws IndexOutOfBoundsException 步骤序号超出范围时抛出
     */
    public CheckHandler getNode(int stepOrder) {
        return nodes.get(stepOrder - 1);
    }

    /**
     * 获取总步骤数
     */
    public int getTotalSteps() {
        return nodes.size();
    }

    /**
     * 根据步骤序号获取步骤元信息
     */
    public StepInfo getStepInfo(int stepOrder) {
        return stepInfos.get(stepOrder - 1);
    }
}
