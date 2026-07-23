-- ============================================================
-- 物业账单表
-- 用于存储物业导入的业主账单数据（物业费、水费、电费等）
-- ============================================================

CREATE TABLE IF NOT EXISTS property_bill (
    id                 BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    building_no        VARCHAR(20)   NOT NULL                  COMMENT '楼栋号',
    unit_no            VARCHAR(20)   NOT NULL                  COMMENT '单元号',
    room_no            VARCHAR(20)   NOT NULL                  COMMENT '房号',
    owner_name         VARCHAR(100)  DEFAULT NULL              COMMENT '业主姓名',
    phone              VARCHAR(20)   DEFAULT NULL              COMMENT '联系电话',
    fee_type           VARCHAR(50)   NOT NULL                  COMMENT '费用类型（物业费/水费/电费/停车费/垃圾清运费/维修费/其他）',
    billing_start_date DATE          NOT NULL                  COMMENT '计费起始日期',
    billing_end_date   DATE          NOT NULL                  COMMENT '计费截止日期',
    amount_due         DECIMAL(10,2) NOT NULL                  COMMENT '应收金额',
    amount_paid        DECIMAL(10,2) NOT NULL DEFAULT 0        COMMENT '已收金额',
    due_date           DATE          NOT NULL                  COMMENT '缴费截止日期',
    status             VARCHAR(20)   NOT NULL DEFAULT 'UNPAID' COMMENT '账单状态：UNPAID-未缴、PAID-已缴、PARTIAL-部分缴',
    remark             VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    created_by         BIGINT        DEFAULT NULL              COMMENT '创建人ID',
    created_at         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_room     (building_no, unit_no, room_no),
    INDEX idx_fee_type (fee_type),
    INDEX idx_status   (status),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物业账单表';


-- ============================================================
-- 物业账单导入记录表
-- 用于记录每次 Excel 导入操作的审计跟踪信息
-- ============================================================

CREATE TABLE IF NOT EXISTS property_bill_import_record (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id       VARCHAR(64)   NOT NULL UNIQUE            COMMENT '任务唯一标识（UUID）',
    file_name     VARCHAR(255)  NOT NULL                   COMMENT '原始文件名',
    total_rows    INT           NOT NULL DEFAULT 0         COMMENT '总行数（不含表头）',
    success_rows  INT           NOT NULL DEFAULT 0         COMMENT '成功导入行数',
    fail_rows     INT           NOT NULL DEFAULT 0         COMMENT '失败行数',
    fail_detail   JSON          DEFAULT NULL               COMMENT '失败行明细，JSON数组格式：[{"row":3,"field":"amountDue","value":"-100","reason":"应收金额必须大于0"}]',
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-排队中、PROCESSING-处理中、SUCCESS-成功、FAIL-失败',
    cost_time_ms  BIGINT        DEFAULT NULL               COMMENT '处理耗时（毫秒）',
    created_by    BIGINT        DEFAULT NULL               COMMENT '操作人ID',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_status   (status),
    INDEX idx_created_by (created_by),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物业账单导入记录表';
