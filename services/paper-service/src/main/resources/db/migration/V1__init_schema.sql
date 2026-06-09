CREATE TABLE IF NOT EXISTS paper (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    total_score INT DEFAULT 0,
    duration_minutes INT DEFAULT 0,
    description TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subject (subject_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

CREATE TABLE IF NOT EXISTS paper_section (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paper_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL COMMENT '如: 一、选择题',
    sort INT DEFAULT 0,
    total_score INT DEFAULT 0,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_paper (paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大题板块表';

CREATE TABLE IF NOT EXISTS paper_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort INT DEFAULT 0,
    score INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_section_question (section_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

CREATE TABLE IF NOT EXISTS paper_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    total_score INT NOT NULL DEFAULT 0,
    description TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组卷模板表';

CREATE TABLE IF NOT EXISTS template_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    section_title VARCHAR(200) NOT NULL COMMENT '板块标题: 一、选择题',
    question_type VARCHAR(30) NOT NULL,
    difficulty DECIMAL(2,1) COMMENT '可选难度',
    knowledge_point_ids TEXT COMMENT 'JSON数组, 可选指定知识点',
    question_count INT NOT NULL DEFAULT 1,
    score_per_question INT NOT NULL DEFAULT 0,
    sort INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板规则表';