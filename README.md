# 在线考试系统 (Online Exam System)

基于微服务架构的在线考试平台，支持题库管理、智能组卷、在线考试、自动批改、学情分析。

## 技术栈

**后端：** Spring Boot 3.2 + Spring Cloud Gateway + MyBatis-Plus + Flyway  
**前端：** Vue 3 + TypeScript + Vite + Element Plus + ECharts  
**数据库：** MySQL 8.0 + Redis  
**部署：** Docker Compose

## 系统架构

```
前端 (nginx:80) → Gateway (8080) → Auth Service (8081)   # 认证鉴权
                                  → Question Service (8082) # 题库管理
                                  → Paper Service (8083)    # 试卷/考试/统计
MySQL (3306) + Redis (6379)
```

## 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| `gateway` | 8080 | API 网关，路由分发 + JWT 鉴权 |
| `auth-service` | 8081 | 用户/角色/权限/组织管理 |
| `question-service` | 8082 | 科目/知识点/题目/教材导入 |
| `paper-service` | 8083 | 试卷/组卷模板/考试/批改/统计 |
| `frontend` | 5173 | Vue 3 前端界面 |

## 功能特性

- **题库管理：** 科目管理、知识点管理、多题型题目（单选/多选/判断/填空/主观/综合题）、教材 Word/PDF 导入（含 OCR 图片文字识别）
- **智能组卷：** 模板组卷（按题型/知识点/难度配置）、手工组卷、试卷导出/打印
- **考试管理：** 考试发布（固定时间/限时模式）、学生分配、考试码加入
- **在线考试：** 全屏强制、切屏检测、复制粘贴禁止、答题时间线、自动提交
- **自动批改：** 客观题自动评分、主观题人工批改、成绩发布通知
- **统计分析：** 成绩分布、试卷质量分析（难度/区分度/信度 Cronbach α）、知识点薄弱分析
- **站内通知：** 考试发布提醒、成绩发布通知

## 快速启动

### 环境要求

- JDK 17+
- Node.js 20+
- MySQL 8.0
- Redis 7.x

### 本地运行

```bash
# 1. 创建数据库（MySQL 8.0）
mysql -u root -p -e "CREATE DATABASE auth_db DEFAULT CHARSET utf8mb4;"
mysql -u root -p -e "CREATE DATABASE question_db DEFAULT CHARSET utf8mb4;"
mysql -u root -p -e "CREATE DATABASE paper_db DEFAULT CHARSET utf8mb4;"

# 2. 修改各服务 application.yml 中的数据库连接配置

# 3. 构建后端
mvn package -DskipTests

# 4. 按顺序启动后端服务（各开一个终端）
java -jar services/auth-service/target/*.jar --server.port=8081
java -jar services/question-service/target/*.jar --server.port=8082
java -jar services/paper-service/target/*.jar --server.port=8083
java -jar services/gateway/target/*.jar --server.port=8080

# 5. 启动前端
cd frontend/study-front
npm install
npm run dev

# 6. 访问 http://localhost:5173
```

### Docker 部署

```bash
docker-compose up -d --build
```

访问 `http://localhost` 即可。

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 租户管理员 | admin | admin123 |

## 数据库迁移

项目使用 Flyway 管理数据库版本：

- `auth_db`：用户/角色/权限/组织相关表
- `question_db`：科目/知识点/题目相关表
- `paper_db`：试卷/考试/答案/统计相关表（含 V1-V6 迁移）

## 构建

```bash
# 全量构建
mvn clean package -DskipTests

# 构建单个模块
mvn package -pl services/paper-service -am -DskipTests
```
