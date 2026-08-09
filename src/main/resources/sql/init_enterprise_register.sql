-- ============================================================
-- 企业入驻功能 - 数据库初始化脚本
-- ============================================================

CREATE TABLE IF NOT EXISTS enterprise_type (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name        VARCHAR(50)   NOT NULL                   COMMENT '类型名称',
    code        VARCHAR(50)   NOT NULL UNIQUE            COMMENT '类型编码',
    description VARCHAR(255)  DEFAULT NULL               COMMENT '类型描述',
    sort        INT           NOT NULL DEFAULT 0         COMMENT '排序号',
    status      VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用、DISABLED-禁用',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_sort   (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业类型配置表';

CREATE TABLE IF NOT EXISTS check_item_def (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name          VARCHAR(100)  NOT NULL                   COMMENT '校验项名称',
    code          VARCHAR(50)   NOT NULL UNIQUE            COMMENT '校验项编码',
    handler_bean  VARCHAR(100)  NOT NULL                   COMMENT '对应的Spring Bean名称',
    description   VARCHAR(255)  DEFAULT NULL               COMMENT '校验项描述',
    sort          INT           NOT NULL DEFAULT 0         COMMENT '排序号',
    status        VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用、DISABLED-禁用',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_sort   (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校验项定义表';

CREATE TABLE IF NOT EXISTS enterprise_type_check (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_id         BIGINT        NOT NULL                   COMMENT '企业类型ID',
    check_item_id   BIGINT        NOT NULL                   COMMENT '校验项ID',
    step_order      INT           NOT NULL                   COMMENT '步骤序号（从1开始）',
    status          VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用、DISABLED-禁用',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    UNIQUE KEY uk_type_step (type_id, step_order),
    INDEX idx_type_id (type_id),
    INDEX idx_check_item_id (check_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业类型-校验项关联表';

CREATE TABLE IF NOT EXISTS enterprise_register (
    id                 BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    enterprise_name    VARCHAR(200)  NOT NULL                   COMMENT '企业名称',
    type_id            BIGINT        NOT NULL                   COMMENT '企业类型ID',
    unified_code       VARCHAR(50)   DEFAULT NULL               COMMENT '统一社会信用代码',
    legal_person       VARCHAR(100)  DEFAULT NULL               COMMENT '法定代表人',
    legal_person_phone VARCHAR(20)   DEFAULT NULL               COMMENT '法人联系电话',
    contact_name       VARCHAR(100)  DEFAULT NULL               COMMENT '联系人',
    contact_phone      VARCHAR(20)   DEFAULT NULL               COMMENT '联系电话',
    contact_email      VARCHAR(100)  DEFAULT NULL               COMMENT '联系邮箱',
    address            VARCHAR(500)  DEFAULT NULL               COMMENT '企业地址',
    current_step       INT           NOT NULL DEFAULT 1         COMMENT '当前进行到的步骤序号',
    status             VARCHAR(30)   NOT NULL DEFAULT 'DRAFT'   COMMENT '入驻状态：DRAFT-草稿、CHECKING-校验中、ALL_CHECKED-全部通过、PENDING_REVIEW-待审核、APPROVED-已通过、REJECTED-已驳回',
    draft_data         JSON          DEFAULT NULL               COMMENT '各步骤草稿数据（JSON格式）',
    reject_reason      VARCHAR(500)  DEFAULT NULL               COMMENT '驳回原因',
    created_by         BIGINT        DEFAULT NULL               COMMENT '创建人ID',
    created_at         DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_type_id     (type_id),
    INDEX idx_status      (status),
    INDEX idx_created_by  (created_by),
    INDEX idx_created_at  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业入驻申请表';

CREATE TABLE IF NOT EXISTS enterprise_check_record (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    register_id     BIGINT        NOT NULL                   COMMENT '入驻申请ID',
    check_item_id   BIGINT        NOT NULL                   COMMENT '校验项ID',
    step_order      INT           NOT NULL                   COMMENT '步骤序号',
    check_status    VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '校验状态：PENDING-待校验、PASSED-通过、FAILED-未通过',
    check_result    JSON          DEFAULT NULL               COMMENT '校验结果详情（JSON）',
    checked_by      BIGINT        DEFAULT NULL               COMMENT '校验人ID',
    checked_at      DATETIME      DEFAULT NULL               COMMENT '校验时间',
    remark          VARCHAR(500)  DEFAULT NULL               COMMENT '备注',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_register_id   (register_id),
    INDEX idx_check_status  (check_status),
    UNIQUE KEY uk_register_step (register_id, step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校验记录表';

-- 初始数据
INSERT INTO enterprise_type (name, code, description, sort) VALUES
    ('科技研发类', 'TECH_RD', '科技研发类企业', 1),
    ('金融类', 'FINANCE', '金融类企业', 2)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO check_item_def (name, code, handler_bean, description, sort) VALUES
    ('营业执照校验', 'BUSINESS_LICENSE', 'businessLicenseHandler', '校验营业执照真实性、有效性', 1),
    ('法人信息校验', 'LEGAL_PERSON', 'legalPersonHandler', '校验法定代表人信息真实性', 2),
    ('金融许可证校验', 'FINANCIAL_LICENSE', 'financialLicenseHandler', '校验金融许可证有效性', 3)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO enterprise_type_check (type_id, check_item_id, step_order) VALUES
    ((SELECT id FROM enterprise_type WHERE code = 'TECH_RD'),
     (SELECT id FROM check_item_def WHERE code = 'BUSINESS_LICENSE'), 1),
    ((SELECT id FROM enterprise_type WHERE code = 'TECH_RD'),
     (SELECT id FROM check_item_def WHERE code = 'LEGAL_PERSON'), 2),
    ((SELECT id FROM enterprise_type WHERE code = 'FINANCE'),
     (SELECT id FROM check_item_def WHERE code = 'BUSINESS_LICENSE'), 1),
    ((SELECT id FROM enterprise_type WHERE code = 'FINANCE'),
     (SELECT id FROM check_item_def WHERE code = 'LEGAL_PERSON'), 2),
    ((SELECT id FROM enterprise_type WHERE code = 'FINANCE'),
     (SELECT id FROM check_item_def WHERE code = 'FINANCIAL_LICENSE'), 3)
ON DUPLICATE KEY UPDATE step_order = VALUES(step_order);
