-- ============================================================
-- 企业入驻校验模块 - 建表脚本
-- 数据库：MySQL 5.7+ / 8.0+
-- 字符集：utf8mb4
-- 说明：created_at / updated_at 与项目实体 createdAt / updatedAt 对应
-- ============================================================

-- 1. 校验步骤配置表
-- 定义不同企业类型的校验步骤序列，支持配置化驱动
CREATE TABLE validation_config (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_type_id   BIGINT          NOT NULL COMMENT '企业类型 ID',
    strategy_id         VARCHAR(50)     NOT NULL COMMENT '业务策略标识符（如 BUSINESS_LICENSE）',
    step_order          INT             NOT NULL COMMENT '步骤序号（从 1 开始，连续递增）',
    config_version      INT             NOT NULL COMMENT '配置版本号',
    enabled             TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_config_type_version (enterprise_type_id, config_version),
    INDEX idx_config_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业入驻校验步骤配置表';

-- 2. 配置版本号管理表
-- 独立管理各企业类型的配置版本号，支持原子自增
CREATE TABLE enterprise_config_version (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    type_id             BIGINT          NOT NULL COMMENT '企业类型 ID',
    current_version     INT             NOT NULL DEFAULT 0 COMMENT '当前版本号',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_id (type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业入驻校验配置版本号管理表';

-- 3. 入驻申请表
-- 记录用户的入驻流程状态，包含配置版本快照
CREATE TABLE enterprise_register (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_name     VARCHAR(200)    NOT NULL COMMENT '企业名称',
    unified_code        VARCHAR(50)     DEFAULT NULL COMMENT '统一社会信用代码',
    type_id             BIGINT          NOT NULL COMMENT '企业类型 ID',
    current_step        INT             NOT NULL DEFAULT 0 COMMENT '当前步骤序号',
    config_version      INT             NOT NULL COMMENT '入驻开始时锁定的配置版本（快照）',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/CHECKING/ALL_CHECKED/PENDING_REVIEW/APPROVED/REJECTED/EXPIRED',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_register_type (type_id),
    INDEX idx_register_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业入驻申请表';

-- 4. 校验记录表
-- 记录每个步骤的校验结果，支持回退和进度查询
CREATE TABLE enterprise_check_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    register_id         BIGINT          NOT NULL COMMENT '入驻申请 ID',
    step_order          INT             NOT NULL COMMENT '步骤序号',
    strategy_id         VARCHAR(50)     NOT NULL COMMENT '业务策略标识符',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PASSED/FAILED',
    form_data           JSON            DEFAULT NULL COMMENT '用户提交的表单数据',
    check_result        JSON            DEFAULT NULL COMMENT '校验结果详情',
    error_code          VARCHAR(50)     DEFAULT NULL COMMENT '错误码',
    error_message       VARCHAR(500)    DEFAULT NULL COMMENT '错误描述',
    operator_id         BIGINT          DEFAULT NULL COMMENT '操作人 ID',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_record_register (register_id),
    INDEX idx_record_register_step (register_id, step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业入驻校验记录表';

-- ============================================================
-- 初始化数据（示例）
-- ============================================================

-- 初始化版本号（假设有两种企业类型：1-科技研发类，2-金融类）
INSERT INTO enterprise_config_version (type_id, current_version) VALUES
(1, 1),
(2, 1);

-- 科技研发类（type_id=1）的校验步骤：营业执照 → 法人信息 → 黑名单
INSERT INTO validation_config (enterprise_type_id, strategy_id, step_order, config_version) VALUES
(1, 'BUSINESS_LICENSE',    1, 1),
(1, 'LEGAL_PERSON',        2, 1),
(1, 'BLACKLIST',           3, 1);

-- 金融类（type_id=2）的校验步骤：营业执照 → 法人信息 → 金融许可证 → 黑名单
INSERT INTO validation_config (enterprise_type_id, strategy_id, step_order, config_version) VALUES
(2, 'BUSINESS_LICENSE',    1, 1),
(2, 'LEGAL_PERSON',        2, 1),
(2, 'FINANCIAL_LICENSE',   3, 1),
(2, 'BLACKLIST',           4, 1);
