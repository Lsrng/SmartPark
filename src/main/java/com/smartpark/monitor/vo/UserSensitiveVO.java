package com.smartpark.monitor.vo;

import lombok.Data;

/**
 * 用户敏感信息展示 VO（按权限脱敏后的数据）
 */
@Data
public class UserSensitiveVO {

    private Long userId;

    private String username;

    /** 脱敏手机号，如 138****8000 */
    private String phone;

    /** 脱敏证件号，如 1101**********1234 */
    private String idCard;
}
