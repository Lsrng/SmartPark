package com.smartpark.enterprise.handler;

/**
 * 校验处理器接口 — 责任链中的节点
 * <p>
 * 每个校验项实现此接口，通过 Spring Bean 名称与 check_item_def.handler_bean 关联。
 * 节点间不持有 next 引用，由 StepEngine 控制执行顺序。
 * </p>
 */
public interface CheckHandler {

    /**
     * 执行校验
     *
     * @param request 校验请求
     * @return 校验结果
     */
    CheckResult check(CheckRequest request);
}
