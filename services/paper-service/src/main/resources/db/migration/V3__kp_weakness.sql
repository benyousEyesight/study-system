CREATE TABLE IF NOT EXISTS kp_weakness (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    knowledge_point_name VARCHAR(200),
    total_score DECIMAL(10,2) DEFAULT 0 COMMENT '该知识点总分',
    earned_score DECIMAL(10,2) DEFAULT 0 COMMENT '该知识点得分',
    attempt_count INT DEFAULT 0 COMMENT '题目数量',
    computed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_kp (student_id, knowledge_point_id),
    INDEX idx_student_subject (student_id, subject_id),
    INDEX idx_subject (subject_id),
    INDEX idx_kp (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生知识点掌握统计';
