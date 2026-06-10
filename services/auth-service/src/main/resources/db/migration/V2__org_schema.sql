CREATE TABLE IF NOT EXISTS grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '年级名称（高一/高二/高三）',
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级表';

CREATE TABLE IF NOT EXISTS clazz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '班级名称（如 1班、3班）',
    head_teacher_id BIGINT COMMENT '班主任user_id',
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_grade (grade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

CREATE TABLE IF NOT EXISTS student_enrollment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clazz_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL COMMENT '学生user_id',
    academic_year VARCHAR(20) COMMENT '学年 如 2025-2026',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/GRADUATED/TRANSFERRED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_clazz (clazz_id),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生表';

CREATE TABLE IF NOT EXISTS teacher_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clazz_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL COMMENT '教师user_id',
    subject_id BIGINT COMMENT '任教学科',
    academic_year VARCHAR(20) COMMENT '学年',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_clazz (clazz_id),
    INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任课教师表';
