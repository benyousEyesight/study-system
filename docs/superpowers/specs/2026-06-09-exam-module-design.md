# 在线考试模块设计文档

> 日期: 2026-06-09
> 状态: 已批准待实施

## 1. 概述

在现有 paper-service 中新增在线考试模块，支持考试安排、在线答题、自动批改（客观题）、人工批改（主观题）、成绩发布、防切屏检测等功能。

### 架构决策

- **合并到 paper-service**：考试功能作为 paper-service 内的独立功能包，不新增微服务
- **功能分包**：paper/ exam/ grading/ 三个独立包，方便后期拆分为独立服务

## 2. 数据模型

### 2.1 exam — 考试安排

```sql
CREATE TABLE IF NOT EXISTS exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
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
```

### 2.2 exam_assignment — 考试分配

```sql
CREATE TABLE IF NOT EXISTS exam_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    assign_type VARCHAR(20) NOT NULL COMMENT 'USER/ROLE/EXAM_CODE',
    assignee_id BIGINT COMMENT 'user_id 或 role_id',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exam (exam_id),
    INDEX idx_assignee (assign_type, assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试分配表';
```

### 2.3 exam_session — 考试会话

```sql
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
```

### 2.4 exam_answer — 作答记录

```sql
CREATE TABLE IF NOT EXISTS exam_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
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
```

### 2.5 实体关系

```
Paper 1 ──→ N Exam 1 ──→ N ExamAssignment
                       1 ──→ N ExamSession 1 ──→ N ExamAnswer
```

ExamSession 在开始时创建 `paper_snapshot`（LONGTEXT JSON），完整复制试卷结构，防止考试中修改题目。

### 2.6 自动批改规则

| 题型 | 批改方式 | 判定逻辑 |
|------|----------|----------|
| SINGLE_CHOICE | 自动 | answer_json == question.answer_json |
| MULTIPLE_CHOICE | 自动 | 数组元素完全匹配（顺序无关） |
| TRUE_FALSE | 自动 | answer_json == question.answer_json |
| FILL_BLANK | 自动 | 精确字符串匹配 |
| SHORT_ANSWER | 人工 | 教师打分 + 评语 |
| ESSAY | 人工 | 教师打分 + 评语 |
| COMPOSITE | 混合 | 子题按各自规则 |

### 2.7 会话状态机

```
IN_PROGRESS ──提交──→ SUBMITTED ──开始批改──→ GRADING ──全部完成──→ GRADED
                  ↘ AUTO_SUBMITTED ──→ (同上)
    CANCELLED ←──────────── 教师取消
```

## 3. API 设计

### 3.1 考试管理（教师端）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/exams | 创建考试安排 |
| PUT | /api/exams/{id} | 更新考试安排 |
| DELETE | /api/exams/{id} | 删除考试安排 |
| GET | /api/exams/page | 考试列表（分页） |
| GET | /api/exams/{id} | 考试详情 |
| PUT | /api/exams/{id}/status | 变更考试状态 |
| POST | /api/exams/{id}/assignments | 批量分配考生 |
| GET | /api/exams/{id}/assignments | 查看已分配用户 |
| DELETE | /api/exams/{examId}/assignments/{id} | 移除某个分配 |

### 3.2 学生端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/my-exams | 我的待考/已考列表 |
| POST | /api/my-exams/{examId}/join | 通过考试码加入 |
| POST | /api/my-exams/{examId}/start | 开始考试（创建 session + 快照） |
| GET | /api/my-exams/sessions/{sessionId} | 考试页面数据 |
| POST | /api/my-exams/sessions/{sessionId}/answer | 保存单题答案 |
| POST | /api/my-exams/sessions/{sessionId}/submit | 提交全卷 |
| GET | /api/my-exams/sessions/{sessionId}/result | 查看成绩 |
| POST | /api/my-exams/sessions/{sessionId}/heartbeat | 心跳同步 |

### 3.3 批改

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/grading/exams | 待批改考试列表 |
| GET | /api/grading/exams/{examId}/sessions | 某考试下的会话 |
| GET | /api/grading/sessions/{sessionId} | 某学生答卷详情 |
| POST | /api/grading/sessions/{sessionId}/grade | 提交批改 |
| POST | /api/grading/sessions/{sessionId}/release | 发布成绩 |

## 4. 防作弊设计

### 4.1 当前实现 — 切屏检测

- 前端监听 `document.visibilitychange` 事件
- 每次切出页面记录一次切屏，通过心跳上报到后端
- 超过配置阈值（exam.max_tab_switches）时自动提交试卷

### 4.2 扩展架构

`anti_cheat_config` 字段以 JSON 存储配置，支持未来扩展：

```json
{
  "tab_switch": { "enabled": true, "max_count": 3, "action": "SUBMIT" },
  "ip_restriction": { "enabled": false },
  "face_detection": { "enabled": false }
}
```

新增防作弊策略只需：
1. 定义新的策略处理器实现统一接口
2. 在配置中添加对应键名
3. 管理员通过配置开关一键启用

## 5. 前端页面

### 5.1 菜单结构

```
试卷管理
  ├── 试卷列表       ← 已有
  ├── 创建试卷       ← 已有
  ├── 组卷模板       ← 已有
  ├── 创建模板       ← 已有
  ├── 考试安排       ← 新增（教师）
  └── 我的考试        ← 新增（学生入口）
```

### 5.2 新增页面

1. **考试安排列表** (`/exams`) — 教师端，表格 + 筛选 + 状态切换
2. **创建/编辑考试** (`/exams/create`, `/exams/:id/edit`) — 基本信息 + 选择试卷 + 分配考生 + 时间设置 + 防作弊设置
3. **考试详情** (`/exams/:id`) — 考试信息 + 分配列表 + 成绩概览
4. **在线答题** (`/exam/session/:sessionId`) — 左侧题目导航 + 右侧作答区 + 顶部倒计时 + 交卷按钮
5. **我的考试** (`/my-exams`) — 待考/已考列表 + 考试码加入
6. **批改界面** (`/grading/exams/:examId`) — 按考试查看 → 选学生 → 逐题批改
7. **成绩查看** (`/exam/result/:sessionId`) — 总分 + 各题得分 + 正确答案 + 评语

### 5.3 答题交互

| 题型 | 控件 |
|------|------|
| SINGLE_CHOICE | Radio |
| MULTIPLE_CHOICE | Checkbox |
| TRUE_FALSE | Radio（对/错） |
| FILL_BLANK | Input |
| SHORT_ANSWER | Textarea |
| ESSAY | Textarea |
| COMPOSITE | 子题混合控件 |

## 6. 考试流程时序

```
Teacher                              Student
  │                                     │
  │  1. 创建考试安排                      │
  │  2. 选择试卷 + 分配考生               │
  │  3. 发布考试 ────────→               │
  │                                     │ 4. 查看我的考试
  │                                     │ 5. 开始考试（创建session+快照）
  │                                     │ 6. 逐题作答（自动保存）
  │                                     │ 7. 提交 / 超时自动提交  ──→ 客观题自动批改
  │  8. 查看待批改列表 ←───────────────────│
  │  9. 逐题批改主观题                     │
  │  10. 发布成绩 ────────→               │
  │                                     │ 11. 查看最终成绩
```

## 7. 补充细节

### 7.1 网关路由更新

在 gateway 的 `application.yml` 中添加：

```yaml
- id: paper-service
  uri: http://localhost:8083
  predicates:
    - Path=/api/papers/**,/api/paper-templates/**,/api/exams/**,/api/my-exams/**,/api/grading/**
```

### 7.2 试卷快照（Paper Snapshot）

`paper_snapshot` 在 `POST /start` 时生成，包含：
- 试卷基本信息和板块结构
- 每个题目的完整内容（从 question_db 跨库查询题目 content_json、answer_json、type）
- 题目选项的随机排列（可选，未来防作弊策略）

### 7.3 COMPOSITE 组合题处理

组合题包含多个子题，子题可能混合客观题和主观题：
- `exam_answer` 中每条记录对应当前子题
- 新增字段 `parent_question_id` 标记父题
- 子题的 `question_type` 使用各自的独立类型
- 批改时子题独立处理

### 7.4 切屏检测触发机制

- 前端检测到 `visibilitychange` 后：本地计数 + 通过心跳接口上报
- 服务端收到心跳时：对比 `tab_switch_count >= max_tab_switches` → 自动提交试卷（状态变为 AUTO_SUBMITTED）
- 提交时再次校验切屏次数
- `max_tab_switches = 0` 时完全禁用检测

## 8. 文件组织结构

```
services/paper-service/src/main/java/com/study/paper/
├── PaperApplication.java
├── common/                    ← 已有
├── config/                    ← 已有
├── paper/                     ← 已有
├── exam/
│   ├── controller/
│   │   ├── ExamController.java        （考试管理）
│   │   ├── MyExamController.java       （学生端）
│   │   └── GradingController.java      （批改）
│   ├── service/
│   │   ├── ExamService.java
│   │   ├── ExamSessionService.java
│   │   └── GradingService.java
│   ├── mapper/
│   │   ├── ExamMapper.java
│   │   ├── ExamAssignmentMapper.java
│   │   ├── ExamSessionMapper.java
│   │   └── ExamAnswerMapper.java
│   └── model/
│       ├── entity/
│       │   ├── Exam.java
│       │   ├── ExamAssignment.java
│       │   ├── ExamSession.java
│       │   └── ExamAnswer.java
│       └── dto/
│           ├── ExamCreateDTO.java
│           ├── ExamQueryDTO.java
│           ├── ExamVO.java
│           ├── ExamSessionVO.java
│           ├── AnswerSubmitDTO.java
│           ├── GradeSubmitDTO.java
│           └── ...
└── resources/
    └── db/migration/
        └── V2__exam_schema.sql
```

```
frontend/study-front/src/
├── api/
│   └── exam.ts              ← 新增 API 模块
├── views/
│   └── exam/                ← 新增目录
│       ├── ExamListView.vue
│       ├── ExamCreateView.vue
│       ├── ExamDetailView.vue
│       ├── MyExamsView.vue
│       ├── ExamSessionView.vue  （答题核心页）
│       ├── ExamResultView.vue
│       ├── GradingExamListView.vue
│       ├── GradingSessionView.vue
│       └── components/
│           ├── QuestionRenderer.vue   （根据题型渲染控件）
│           ├── QuestionNavigator.vue   （左侧导航）
│           └── ExamTimer.vue          （倒计时组件）
└── router/
    └── index.ts             ← 新增路由
```
