CREATE TABLE IF NOT EXISTS paper_quality (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL UNIQUE,
    paper_id BIGINT NOT NULL,
    difficulty_index DECIMAL(5,2) COMMENT '难度系数 0-1，越小越难',
    discrimination_index DECIMAL(5,2) COMMENT '区分度 0-1',
    reliability_index DECIMAL(5,2) COMMENT '信度(Cronbach α) 0-1',
    total_questions INT DEFAULT 0,
    total_students INT DEFAULT 0,
    details JSON COMMENT '每题质量数据',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exam (exam_id),
    INDEX idx_paper (paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷质量分析';
