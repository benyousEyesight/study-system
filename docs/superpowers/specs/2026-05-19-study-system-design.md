# 在线教育考试系统 - 设计文档

## 概述

SaaS 多租户在线教育考试平台，支持学校/培训机构独立使用。系统涵盖题库管理、试卷管理、在线考试、统计分析四大核心业务模块，基于微服务架构搭建。

## 技术栈

| 层次 | 技术选型 |
|------|---------|
| 前端 | Vue 3 + TypeScript + Vite + Pinia + Element Plus + ECharts |
| 后端 | Spring Boot 3.x + Spring Cloud Gateway + Spring Security |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8.x（每个服务独立数据库） |
| 缓存 | Redis 7.x |
| 消息队列 | RabbitMQ |
| 服务注册/发现 | Nacos |
| 文件存储 | MinIO（私有化部署） |
| OCR | Tesseract + AI 解析模型 |
| 部署 | Docker + Docker Compose |

## 微服务架构

```
                         ┌─────────────────────────┐
                         │    Nginx (+ HTTPS)       │
                         └────────────┬────────────┘
                                      │
                         ┌────────────▼────────────┐
                         │   Spring Cloud Gateway   │
                         │  (路由 / JWT鉴权 / 限流)  │
                         └────┬────┬────┬────┬────┘
                              │    │    │    │
              ┌───────────────┼────┼────┼────┼───────────────┐
              │               │    │    │    │               │
         ┌────▼───┐    ┌─────▼──┐ ┌──▼───┐ ┌▼──────┐  ┌───▼────┐
         │  Auth  │    │Question│ │ Paper│ │ Exam  │  │ Stats  │
         │ Service│    │  Bank  │ │Service│ │Service│  │Service │
         │        │    │ Service│ │      │ │       │   │        │
         ├────────┤    ├────────┤ ├──────┤ ├───────┤  ├────────┤
         │ Redis  │    │ MySQL  │ │MySQL │ │MySQL  │  │ MySQL  │
         │ MySQL  │    │        │ │      │ │ Redis  │  │        │
         └────────┘    └────────┘ └──────┘ └───────┘  └────────┘
                              │         │
                              └────┬────┘
                                   │
                          ┌────────▼────────┐
                          │   RabbitMQ      │
                          │ (异步事件/统计)  │
                          └─────────────────┘
```

### 服务间通信

- **同步查询**：OpenFeign + gRPC（高频低延迟场景）
- **异步事件**：RabbitMQ（考试提交 → 统计计算，解耦重操作）

### 多租户方案

所有服务统一使用 `tenant_id` 字段进行数据隔离，共用数据库实例。预留租户路由中间件，所有请求在 Gateway 层解析 JWT 获取 tenant_id，注入请求上下文。

## 模块一：Auth & RBAC（Auth Service）

### 数据模型

```
tenant (租户)
  ├── user (用户, 含 super_admin / tenant_admin / teacher / student)
  │   ├── role (角色)
  │   │   └── permission (权限)
  │   └── user_role (用户-角色关联)
  └── role_permission (角色-权限关联)
```

### 表结构

**tenant**：id, name, code(唯一编码), contact_name, contact_phone, status, expire_at, created_at

**user**：id, tenant_id, username, password_hash, real_name, email, phone, avatar, user_type(SUPER_ADMIN/TENANT_ADMIN/TEACHER/STUDENT), status, created_at

**role**：id, tenant_id, name, code, description, is_system(系统内置不可删), status

**permission**：id, code, name, type(API/MENU/BUTTON), parent_id, path, method, sort

### RBAC 设计

- **平台级**：SUPER_ADMIN — 管理所有租户，不可由租户操作
- **租户级**：TENANT_ADMIN（本校全权限）
- **业务级**：TEACHER（题库/试卷/考试/所教班级统计）、STUDENT（参加考试/个人成绩）
- 租户管理员可自定义角色（如"年级组长"、"教研组长"），从已有权限池中组合

### 认证流程

1. 用户登录 → 校验用户名密码 → 生成 JWT（payload: userId, tenantId, roles）
2. JWT 包含 access_token（30min）+ refresh_token（7天）
3. Gateway 统一校验 JWT，解析后通过 Header 透传 userId/tenantId 给下游
4. 登出时将 token 加入 Redis 黑名单
5. Redis 缓存用户权限集（`auth:perm:{userId}`），减少数据库查询

## 模块二：班级与年级管理（Shared / Auth Service）

班级管理是考试分配和统计聚合的基础，由 Auth Service 统一管理。

### 数据模型

```
tenant (租户)
  └── grade (年级，如"高一")
       └── clazz (班级，如"高一(3)班")
            ├── student_enrollment (班级学生)
            └── teacher_assignment (任课教师)
```

### 表结构

**grade**：id, tenant_id, name(高一/高二/高三), sort, status

**clazz**：id, tenant_id, grade_id, name(班级名), head_teacher_id(user_id), sort, status

**student_enrollment**：id, clazz_id, student_id(user_id), academic_year, status(ACTIVE/GRADUATED/TRANSFERRED)

**teacher_assignment**：id, clazz_id, teacher_id(user_id), subject_id, academic_year — 一个教师可教多个班级、多个科目

年级和班级管理由租户管理员维护，教师在发起考试时从已管理的班级中选择参与者。

---

## 模块三：题库管理（Question Bank Service）

### 数据模型

```
subject (科目)
  └── knowledge_point (知识点, 树形)
       └── question (题目)
            ├── question_kp (题目-知识点关联)
            └── composite_structure (组合题子题)
```

### 表结构

**subject**：id, tenant_id, name, code, icon, sort, status

**knowledge_point**：id, subject_id, name, parent_id, level(层级), sort, status — 支持无限层级树

**question**：id, tenant_id, subject_id, type, difficulty(decimal 1-3), content_json, answer_json, analysis(解析), status(DRAFT/PUBLISHED/ARCHIVED), created_by, created_at

**question_kp**：question_id, knowledge_point_id

**composite_structure**：id, parent_question_id, child_question_id, sort, score

### 题型枚举

| type | 描述 | 判分 |
|------|------|------|
| SINGLE_CHOICE | 单选题 | 自动 |
| MULTIPLE_CHOICE | 多选题 | 自动（全对得分，少选/多选不得分） |
| TRUE_FALSE | 判断题 | 自动 |
| FILL_BLANK | 填空题 | 自动（支持别名匹配）/ 人工复核 |
| SHORT_ANSWER | 简答题 | 人工 |
| ESSAY | 论述题/作文 | 人工 |
| COMPOSITE | 组合题（阅读/完形） | 按子题判分 |

### content_json 设计

```json
// 单选题
{ "text": "以下哪个是质数？", "options": {"A": "4", "B": "6", "C": "7", "D": "9"}, "images": [] }

// 阅读理解（组合题）
{ "passage": "文章正文...", "images": [], "passage_type": "READING" }

// 填空题
{ "text": "圆的面积公式是 S = ____", "accept_aliases": ["πr²", "pi*r^2", "3.14*r*r"] }
```

### OCR 导入流程

```
上传文件 → 文件存储(MinIO) → OCR识别(图片用Tesseract/云端OCR, 文档用PDFBox/POI)
  → AI结构化解析 → 人工校对页面 → 确认入库(状态=草稿→已发布)
```

- 支持格式：JPG/PNG（扫描件）、PDF、Word（.docx）
- 导入后题目状态为 DRAFT，需要教师审核发布
- OCR 结果置信度低于阈值的题目标记为待人工处理

## 模块三：试卷管理（Paper Service）

### 数据模型

```
paper (试卷)
  └── paper_section (大题板块)
       └── paper_question (题目+分值)

paper_template (组卷模板)
  └── template_rule (规则行: 题型+知识点+难度+数量+分值)
```

### 表结构

**paper**：id, tenant_id, subject_id, title, total_score, duration_minutes, description, status(DRAFT/PUBLISHED), created_by, created_at

**paper_section**：id, paper_id, title(如"一、选择题"), sort, description, total_score

**paper_question**：id, section_id, question_id, sort, score

**paper_template**：id, tenant_id, subject_id, name, total_score, status

**template_rule**：id, template_id, section_title, question_type, difficulty(decimal, 可选), knowledge_point_ids(JSON, 可选), count(题目数量), score_per_question, sort

### 手动组卷

1. 创建试卷 → 填写基本信息（科目、标题、时长）
2. 添加大题板块（"一、选择题 共40分"）
3. 筛选题库（科目→知识点→题型→难度→关键词）
4. 从结果中勾选题目 → 加入板块 → 设置每题分值
5. 预览 → 调整排序/分值 → 发布

### 自动组卷（模板驱动）

1. 创建模板 → 设置规则
2. 生成引擎根据规则筛选符合条件的题目
3. 按以下策略优化选中的题目：
   - 同知识点避免重复考察
   - 难度分布匹配规则要求
   - 近期考试用过的题目降权
   - 每题只出现一次
4. 预览 → 手动微调（可替换单题）→ 确认生成正式试卷

### 模板示例

| 板块标题 | 题型 | 数量 | 分值/题 | 知识点 | 难度 |
|---------|------|------|--------|-------|------|
| 一、选择题 | SINGLE_CHOICE | 8 | 5 | 函数、导数 | 0.6-0.8 |
| 二、多选题 | MULTIPLE_CHOICE | 3 | 5 | 三角函数、向量 | 0.5-0.7 |
| 三、填空题 | FILL_BLANK | 4 | 5 | 数列、不等式 | 0.4-0.6 |
| 四、解答题 | SHORT_ANSWER | 3 | 10 | 综合 | 0.3-0.5 |

## 模块四：在线考试（Exam Service）

### 数据模型

```
exam (考试活动)
  ├── exam_participant (参与记录)
  │   ├── exam_answer (题目作答)
  │   └── cheating_log (作弊日志)
```

### 表结构

**exam**：id, tenant_id, paper_id, title, start_time, end_time, duration_minutes, status(PENDING/ACTIVE/FINISHED), max_attempts, target_grade_ids(JSON, 目标年级), target_clazz_ids(JSON, 目标班级), anti_cheating_config(JSON), created_by

**exam_participant**：id, exam_id, user_id, status(PENDING/IN_PROGRESS/SUBMITTED/GRADED), started_at, submitted_at, total_score, ip_address

**exam_answer**：id, participant_id, question_id, question_type, answer_json(JSON), score, is_correct, grader_id, graded_at, remark

**cheating_log**：id, exam_id, participant_id, type(TAB_SWITCH/IP_CHANGE/MULTI_DEVICE), detail(JSON), created_at

### 考试全生命周期

**线上考试流程**：

```
[教师端]
  创建考试 → 选择试卷 → 设置时间/防作弊参数 → 指定参与班级/学生 → 发布

[学生端]
  收到考试通知 → 开考前等待 → 进入考试 → 答题(自动保存到Redis) →
  手动提交/时间到自动提交

[批改阶段]
  客观题即时批改 → 教师进入批改界面给主观题打分 →
  所有题批改完成 → 成绩发布(学生可见)

[统计阶段]
  考试完成事件 → MQ → Statistics Service → 异步计算分析数据
```

**线下考试（含成绩 OCR 录入）流程**：

```
[教师端]
  创建考试 → 选择试卷 → 设置考试为"线下模式" → 指定参与班级 → 导出试卷PDF用于打印

[线下执行]
  打印试卷 → 学生纸质作答 → 教师批改/打分

[成绩录入]
  方式A: 使用标准答题卡 → 扫描答题卡 → OMR 识别填涂 → 客观题自动判分
  方式B: 教师直接在纸质答题卡上手写得分 → 扫描录入 →
        OCR 识别每题得分 → 教师确认/修正 → 入库

[统计阶段]
  同线上考试，成绩入库后触发统计计算
```

### 防作弊策略

| 策略 | 实现 | 阈值 | 处理 |
|------|------|------|------|
| 切屏检测 | 浏览器 visibilitychange + 失焦事件 | 允许 3 次 | 第 4 次强制交卷 |
| IP 变化 | 开始和提交时对比 IP | 不匹配则记录 | 标记"IP异常" |
| 同一账号多地登录 | Redis 会话管理 | 新登录顶替旧会话 | 旧会话强制退出 |
| 考试时段锁定 | 考试期间禁止该账号其他操作 | — | 前端路由拦截 + 后端接口拦截 |

### Redis 数据结构

```
// 倒计时（秒）
exam:{examId}:user:{userId}:remaining → 3600

// 临时答案缓存（防止丢数据）
exam:{examId}:user:{userId}:answers → JSON

// 切屏计数
exam:{examId}:user:{userId}:tab_switches → 2

// 在线考生（Set）
exam:{examId}:online_users → Set<userId>
```

### 成绩 OCR 录入

支持线下考试的成绩批量录入，通过扫描纸质答题卡识别分数并自动入库。

**两种录入模式**：

| 模式 | 技术方案 | 适用场景 |
|------|---------|---------|
| **OMR 答题卡识别** | OpenCV 检测填涂区域 → 识别选项 → 比照答案自动判分 | 客观题为主的考试，使用标准答题卡 |
| **手写分数 OCR** | 扫描教师批改后的答题卡 → OCR 识别手写数字 → 提取每题得分 | 主观题为主的考试，教师已手写打分 |

**录入流程**：

```
准备阶段: 系统生成标准答题卡 PDF（含定位标记+学生信息条形码）
          ↓
扫描阶段: 批量扫描答题卡（支持扫描仪/手机拍照）
          ↓
识别阶段: OCR 引擎识别
          ├ 条形码/二维码 → 识别学生身份
          ├ OMR 模式 → 识别涂黑选项 → 自动判分
          └ 手写分数模式 → 识别每道题手写数字 → 提取分数
          ↓
确认阶段: 教师审核识别结果（高亮差异，支持手动修正）
          ↓
入库阶段: 确认无误 → 写入 exam_participant + exam_answer → 触发统计
```

**关键设计点**：

- **标准答题卡模板**：系统根据试卷自动生成答题卡 PDF，包含定位角标、学生信息区（条形码/二维码）、客观题涂卡区、主观题得分区
- **手写数字识别**：针对 0-9 及小数点专项训练，支持 2 位小数
- **置信度标记**：OCR 识别结果附带置信度分数，低于阈值（如 < 85%）高亮提示人工确认
- **批量处理**：支持一次性扫描全班答题卡，系统自动分拣到对应学生
- **容错机制**：同一道题识别结果异常（如得分超过满分）自动标记待人工处理

### 判分流程

- **客观题**（单选/多选/判断）：提交后即时比对标准答案
- **填空题**：提交后即时比对支持别名模糊匹配，未匹配的标记待人工复核
- **主观题**（简答/论述/作文）：教师手动批改打分
- 主观题批改完成后才发布最终成绩

## 模块五：统计分析（Statistics Service）

### 事件驱动架构

```
考试提交/批改完成 → RabbitMQ ExamSubmittedEvent
  → StatsService 消费
    ├ 实时: student_exam_summary（个人成绩汇总）
    ├ 实时: kp_weakness（知识点得分率）
    └ 延迟: exam_report（班级报告，延迟 5min 批量聚合）
       └ 延迟: paper_quality（试卷质量指标，延迟 30min）
```

### 表结构

**student_exam_summary**：id, tenant_id, student_id, exam_id, total_score, rank_in_class, kp_scores(JSON), created_at

**exam_report**：id, exam_id, total_students, avg_score, max_score, min_score, pass_rate(及格率), excellent_rate(优秀率), score_distribution(JSON 分数段分布), created_at

**kp_weakness**：id, tenant_id, student_id, subject_id, knowledge_point_id, avg_score_rate, attempt_count, last_exam_id, updated_at — 持续追踪学生各知识点的掌握情况

**paper_quality**：id, paper_id, exam_id, difficulty_index, discrimination_index, reliability_index, created_at

### 核心指标公式

| 指标 | 公式 | 含义 |
|------|------|------|
| 难度系数 | P = 平均分 / 满分 | P<0.3 偏难, 0.3~0.7 适中, >0.7 偏易 |
| 区分度 | D = 高分组合格率 - 低分组合格率 | D>0.4 优秀, 0.2~0.4 良好, <0.2 需淘汰 |
| 得分率 | 该知识点得分之和 / 该知识点满分之和 | <60% 标识为薄弱点 |
| 信度 | Cronbach's α 系数 | α>0.8 信度良好 |

### 前端可视化

- 学生个人：成绩趋势折线图、知识点掌握雷达图、薄弱知识点排名
- 教师端：班级成绩分布柱状图、试卷质量指标卡片、各知识点班级平均得分率热力图
- 导出格式：Excel 报表（支持按班级/按考试导出）

### 试卷导出与打印

支持将试卷导出为线下考试可用的格式，这是 Paper Service 提供的附加能力。

**导出格式**：

| 格式 | 场景 | 实现方式 |
|------|------|---------|
| PDF | 打印分发给学生 | Apache PDFBox / iText 渲染 |
| Word (.docx) | 教师编辑修改 | Apache POI 模板渲染 |
| HTML | 在线预览打印 | 浏览器原生打印（CSS @media print） |

**排版规则**（可配置模板）：

- 试卷头部：学校名称/Logo、考试标题、总分、时长、注意事项
- 选择题选项对齐（A/B/C/D 纵向排列或两栏排列）
- 答题区域预留（主观题后留空白行）
- 密封线（试卷左侧/顶部装订线，含姓名、班级、学号栏位）
- 页脚：页码 `共 X 页 第 X 页`

**配套导出内容**：

```
试卷导出:
├── 学生卷（仅试题，不含答案）
├── 教师卷（含标准答案和解析）
└── 答题卡（仅答题区域，客观题涂卡区 + 主观题手写区）
```

**导出触发点**：
- 试卷发布后，教师端「导出」按钮
- 支持按需导出（选择导出类型：学生卷/教师卷/答题卡）

---

## 跨领域关注点

### API Gateway 职责

- 路由转发：根据请求路径分发到对应微服务
- JWT 鉴权：Gateway 层统一校验 token，透传用户信息
- 租户上下文：从 JWT 提取 tenant_id 注入 Header
- 限流：按租户/按接口进行流量控制（令牌桶算法）

### 文件存储

- MinIO 私有化部署，存储 OCR 上传的图片/文档
- 文件路径规则：`{tenant_id}/{module}/{date}/{uuid}.{ext}`
- 文件服务独立部署，提供预签名 URL 用于上传和访问

### 部署架构

- 每个微服务容器化（Docker）
- Docker Compose 编排本地开发环境
- 生产环境可迁移至 Kubernetes

## 未来扩展预留

- 消息通知模块（考试提醒、成绩通知）— 可对接微信/邮件/短信
- 课程管理模块 — 将考试与课程体系关联
- 题库协作 — 多位教师协同维护题库
- AI 智能推荐 — 根据薄弱知识点推荐练习题
