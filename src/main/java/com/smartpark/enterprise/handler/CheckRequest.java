package com.smartpark.enterprise.handler;

import com.smartpark.pojo.entity.enterprise.EnterpriseRegister;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 校验请求
 */
@Data
@Builder
@AllArgsConstructor
public class CheckRequest {

    /** 入驻申请信息 */
    private EnterpriseRegister register;

    /** 当前步骤序号 */
    private Integer stepOrder;

    /** 当前步骤提交的表单数据 */
    private Map<String, Object> formData;

    /** 操作人ID */
    private Long operatorId;
}
