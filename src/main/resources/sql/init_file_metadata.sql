-- 文件元信息表
CREATE TABLE IF NOT EXISTS t_file_metadata (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    storage_path    VARCHAR(512)    NOT NULL COMMENT '存储路径（bucket/yyyy/MM/dd/uuid.ext）',
    bucket          VARCHAR(64)     NOT NULL COMMENT '业务分组',
    stored_name     VARCHAR(128)    NOT NULL COMMENT '存储文件名（uuid.ext）',
    original_name   VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    real_extension  VARCHAR(16)     NOT NULL COMMENT '魔数检测的真实扩展名',
    mime_type       VARCHAR(128)    NOT NULL COMMENT '真实 MIME 类型',
    file_size       BIGINT          NOT NULL COMMENT '文件大小（字节）',
    upload_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_storage_path (storage_path),
    INDEX idx_bucket (bucket)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元信息表';
