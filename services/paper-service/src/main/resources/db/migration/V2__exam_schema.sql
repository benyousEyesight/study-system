CREATE TABLE IF NOT EXISTS exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 0,
    paper_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL COMMENT '考试名称',
    description TEXT COMMENT '考试说明',
    exam_code VARCHAR(20) COMMENT '考试码',
    time_mode VARCHAR(20) NOT NULL COMMENT 'FIXED_WINDOW/FLEXIBLE/BOTH',
    start_time DATETIME COMMENT '固定开始时间',
    end_time DATETIME COMMENT '固定结束时间',
    duration_minutes INT COMMENT '作答时长(分钟)',
    max_tab_switches INT DEFAULT 0 COMMENT '0=禁用,-1=不限',
    anti_cheat_config JSON COMMENT '防作弊扩展配置',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/IN_PROGRESS/FINISHED',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试安排表';

CREATE TABLE IF NOT EXISTS exam_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    assign_type VARCHAR(20) NOT NULL COMMENT 'USER/ROLE/EXAM_CODE',
    assignee_id BIGINT COMMENT 'user_id 或 role_id',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exam (exam_id),
    INDEX idx_assignee (assign_type, assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试分配表';

CREATE TABLE IF NOT EXISTS exam_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    paper_snapshot LONGTEXT NOT NULL COMMENT '试卷快照JSON',
    start_time DATETIME NOT NULL,
    submitted_at DATETIME,
    duration_minutes INT COMMENT '实际用时',
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/SUBMITTED/AUTO_SUBMITTED/GRADING/GRADED/CANCELLED',
    tab_switch_count INT DEFAULT 0,
    total_score DECIMAL(10,2) COMMENT '总分',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exam (exam_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试会话表';

CREATE TABLE IF NOT EXISTS exam_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    parent_question_id BIGINT DEFAULT NULL COMMENT '组合题子题归属',
    question_type VARCHAR(30) NOT NULL COMMENT '冗余，方便批改',
    answer_json TEXT COMMENT '学生答案JSON',
    score DECIMAL(10,2) COMMENT '本题得分',
    is_correct TINYINT(1) COMMENT '客观题是否正确',
    grader_id BIGINT COMMENT '批改人',
    grader_comment TEXT COMMENT '教师评语',
    grading_status VARCHAR(20) DEFAULT 'UNGRADED' COMMENT 'UNGRADED/AUTO_GRADED/MANUAL_GRADED',
    answered_at DATETIME,
    graded_at DATETIME,
    INDEX idx_session (session_id),
    INDEX idx_grading (grading_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作答记录表';
