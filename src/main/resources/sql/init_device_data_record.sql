-- ============================================================
-- 物联设备上报数据记录表
-- 用于存储物联设备上报的监测数据
-- ============================================================

CREATE TABLE IF NOT EXISTS device_data_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    data JSON COMMENT '上报数据（JSON格式）',
    report_time DATETIME NOT NULL COMMENT '设备上报时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    INDEX idx_device_id (device_id),
    INDEX idx_report_time (report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备上报数据记录表';
