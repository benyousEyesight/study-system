# 在线考试模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 paper-service 中实现考试安排、在线答题、自动批改、人工批改、防切屏、成绩发布功能

**Architecture:** 在现有 paper-service 中新增 exam/ 功能包，含 controller/service/mapper/model 各层。新增 exam、exam_assignment、exam_session、exam_answer 四张表。网关新增路由。前端新增 8 个页面 + 3 个答题组件。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, MySQL 8.0, Vue 3, Element Plus, TypeScript

---

### 前置检查

在开始任何任务前，验证服务和数据库正在运行：

```bash
# 检查 Docker 容器
docker ps | grep mysql

# 检查 paper-service 数据库表
docker compose -f "D:/workspace/study-system/docker-compose.yml" exec -T mysql mysql -u root -proot123 -e "USE paper_db; SHOW TABLES;"
```

预期：paper_db 中应有 paper、paper_section、paper_question、paper_template、template_rule 五张表。

---

### Task 1: 数据库迁移 SQL + 网关路由

**文件：**
- Create: `services/paper-service/src/main/resources/db/migration/V2__exam_schema.sql`
- Modify: `services/gateway/src/main/resources/application.yml`
- Modify: `services/paper-service/pom.xml`

- [ ] **Step 1: 创建 V2__exam_schema.sql**

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
    anti_cheat_config TEXT COMMENT '防作弊扩展配置JSON',
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
```

- [ ] **Step 2: 执行迁移 SQL**

```bash
docker compose -f "D:/workspace/study-system/docker-compose.yml" exec -T mysql mysql -u root -proot123 paper_db < "D:/workspace/study-system/services/paper-service/src/main/resources/db/migration/V2__exam_schema.sql"
```

验证：
```bash
docker compose -f "D:/workspace/study-system/docker-compose.yml" exec -T mysql mysql -u root -proot123 -e "USE paper_db; SHOW TABLES;"
```

预期新增：exam、exam_assignment、exam_session、exam_answer 四张表。

- [ ] **Step 3: 更新网关路由**

修改 `services/gateway/src/main/resources/application.yml`，将 paper-service 的路由路径扩展：

```yaml
        - id: paper-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/papers/**,/api/paper-templates/**,/api/exams/**,/api/my-exams/**,/api/grading/**
```

- [ ] **Step 4: 提交**

```bash
git add services/paper-service/src/main/resources/db/migration/V2__exam_schema.sql services/gateway/src/main/resources/application.yml
git commit -m "feat(exam): add exam schema and gateway routes"
```

---

### Task 2: Backend 实体类

**文件：**
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/entity/Exam.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/entity/ExamAssignment.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/entity/ExamSession.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/entity/ExamAnswer.java`

- [ ] **Step 1: 创建 Exam.java**

```java
package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam")
public class Exam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long paperId;
    private String title;
    private String description;
    private String examCode;
    private String timeMode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer maxTabSwitches;
    private String antiCheatConfig;
    private String status;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 ExamAssignment.java**

```java
package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_assignment")
public class ExamAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private String assignType;
    private Long assigneeId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 ExamSession.java**

```java
package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_session")
public class ExamSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long userId;
    private String paperSnapshot;
    private LocalDateTime startTime;
    private LocalDateTime submittedAt;
    private Integer durationMinutes;
    private String status;
    private Integer tabSwitchCount;
    private BigDecimal totalScore;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 创建 ExamAnswer.java**

```java
package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long sectionId;
    private Long questionId;
    private Long parentQuestionId;
    private String questionType;
    private String answerJson;
    private BigDecimal score;
    private Integer isCorrect;
    private Long graderId;
    private String graderComment;
    private String gradingStatus;
    private LocalDateTime answeredAt;
    private LocalDateTime gradedAt;
}
```

- [ ] **Step 5: 提交**

```bash
git add services/paper-service/src/main/java/com/study/paper/exam/model/entity/
git commit -m "feat(exam): add exam entities"
```

---

### Task 3: Backend Mappers

**文件：**
- Create: `services/paper-service/src/main/java/com/study/paper/exam/mapper/ExamMapper.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/mapper/ExamAssignmentMapper.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/mapper/ExamSessionMapper.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/mapper/ExamAnswerMapper.java`

- [ ] **Step 1: 创建 ExamMapper.java**

```java
package com.study.paper.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.paper.exam.model.entity.Exam;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
}
```

- [ ] **Step 2: 创建 ExamAssignmentMapper.java**

```java
package com.study.paper.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.paper.exam.model.entity.ExamAssignment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamAssignmentMapper extends BaseMapper<ExamAssignment> {
}
```

- [ ] **Step 3: 创建 ExamSessionMapper.java**

```java
package com.study.paper.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.paper.exam.model.entity.ExamSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamSessionMapper extends BaseMapper<ExamSession> {
}
```

- [ ] **Step 4: 创建 ExamAnswerMapper.java**

```java
package com.study.paper.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.paper.exam.model.entity.ExamAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {

    @Select("SELECT q.id, q.type, q.content_json AS contentJson, q.answer_json AS answerJson, q.difficulty " +
            "FROM question_db.question q WHERE q.id = #{id}")
    Map<String, Object> selectQuestionById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT q.id, q.type, q.content_json AS contentJson, q.answer_json AS answerJson, q.difficulty " +
            "FROM question_db.question q WHERE q.id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=''>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectQuestionsByIds(@Param("ids") List<Long> ids);
}
```

注意：`selectQuestionById` 和 `selectQuestionsByIds` 是跨库查询（paper_db → question_db），复用已有 `ExternalQuestionRefMapper` 的查询模式，但放在 ExamAnswerMapper 中直接使用，因为自动批改时需要读取题目答案做比对。

- [ ] **Step 5: 提交**

```bash
git add services/paper-service/src/main/java/com/study/paper/exam/mapper/
git commit -m "feat(exam): add exam mappers"
```

---

### Task 4: Backend DTOs

**文件：**
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/dto/ExamCreateDTO.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/dto/ExamQueryDTO.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/dto/ExamVO.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/dto/ExamSessionVO.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/dto/AnswerSubmitDTO.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/dto/GradeSubmitDTO.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/model/dto/AssignmentDTO.java`

- [ ] **Step 1: 创建 ExamCreateDTO.java**

```java
package com.study.paper.exam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamCreateDTO {
    @NotNull private Long paperId;
    @NotBlank private String title;
    private String description;
    private String examCode;
    @NotBlank private String timeMode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer maxTabSwitches;
    private String antiCheatConfig;
    private String status;

    private List<AssignmentItem> assignments;

    @Data
    public static class AssignmentItem {
        @NotBlank private String assignType;
        private Long assigneeId;
    }
}
```

- [ ] **Step 2: 创建 ExamQueryDTO.java**

```java
package com.study.paper.exam.model.dto;

import lombok.Data;

@Data
public class ExamQueryDTO {
    private int page = 1;
    private int size = 10;
    private String title;
    private String status;
    private Long tenantId;
}
```

- [ ] **Step 3: 创建 ExamVO.java**

```java
package com.study.paper.exam.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamVO {
    private Long id;
    private Long tenantId;
    private Long paperId;
    private String paperTitle;
    private String title;
    private String description;
    private String examCode;
    private String timeMode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer maxTabSwitches;
    private String antiCheatConfig;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AssignmentVO> assignments;
    private Integer totalSessions;
    private Integer gradedSessions;
}
```

- [ ] **Step 4: 创建 AssignmentVO.java**

```java
package com.study.paper.exam.model.dto;

import lombok.Data;

@Data
public class AssignmentVO {
    private Long id;
    private Long examId;
    private String assignType;
    private Long assigneeId;
    private String assigneeName;
    private String assigneeTypeName;
}
```

- [ ] **Step 5: 创建 ExamSessionVO.java**

```java
package com.study.paper.exam.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ExamSessionVO {
    private Long id;
    private Long examId;
    private Long userId;
    private String studentName;
    private LocalDateTime startTime;
    private LocalDateTime submittedAt;
    private Integer durationMinutes;
    private String status;
    private Integer tabSwitchCount;
    private BigDecimal totalScore;
    private LocalDateTime createdAt;

    // 考试页面数据（开始考试时返回）
    private ExamBasicVO examInfo;
    private List<SectionVO> sections;

    @Data
    public static class ExamBasicVO {
        private String title;
        private Integer durationMinutes;
        private Integer remainingSeconds;
        private Integer maxTabSwitches;
    }

    @Data
    public static class SectionVO {
        private Long id;
        private String title;
        private Integer sort;
        private Integer totalScore;
        private List<QuestionVO> questions;
    }

    @Data
    public static class QuestionVO {
        private Long questionId;
        private Integer sort;
        private Integer score;
        private String type;
        private Map<String, Object> content;
        private String answerSnapshot;
    }

    // 成绩查看时返回
    private List<AnswerResultVO> answers;

    @Data
    public static class AnswerResultVO {
        private Long questionId;
        private String questionType;
        private String questionContent;
        private String correctAnswer;
        private String studentAnswer;
        private BigDecimal score;
        private BigDecimal totalScore;
        private Integer isCorrect;
        private String gradingStatus;
        private String graderComment;
    }
}
```

- [ ] **Step 6: 创建 AnswerSubmitDTO.java**

```java
package com.study.paper.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmitDTO {
    @NotNull private Long questionId;
    private Long parentQuestionId;
    private Long sectionId;
    private String answerJson;
}
```

- [ ] **Step 7: 创建 GradeSubmitDTO.java**

```java
package com.study.paper.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GradeSubmitDTO {
    @NotNull private List<GradeItem> grades;

    @Data
    public static class GradeItem {
        @NotNull private Long questionId;
        @NotNull private BigDecimal score;
        private String comment;
    }
}
```

- [ ] **Step 8: 创建 AssignmentDTO.java**

```java
package com.study.paper.exam.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class AssignmentDTO {
    @NotNull private List<Long> userIds;
    private List<Long> roleIds;
    private String examCode;
}
```

- [ ] **Step 9: 提交**

```bash
git add services/paper-service/src/main/java/com/study/paper/exam/model/dto/
git commit -m "feat(exam): add exam DTOs"
```

---

### Task 5: ExamService — 考试安排 + 分配管理

**文件：**
- Create: `services/paper-service/src/main/java/com/study/paper/exam/service/ExamService.java`

- [ ] **Step 1: 创建 ExamService.java**

```java
package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.paper.exam.mapper.ExamAssignmentMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamAssignment;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.paper.mapper.PaperMapper;
import com.study.paper.paper.model.entity.Paper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamAssignmentMapper assignmentMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private PaperMapper paperMapper;

    public Page<ExamVO> page(ExamQueryDTO query) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Exam::getTitle, query.getTitle());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Exam::getStatus, query.getStatus());
        }
        if (query.getTenantId() != null) {
            wrapper.eq(Exam::getTenantId, query.getTenantId());
        }
        wrapper.orderByDesc(Exam::getCreatedAt);

        Page<Exam> p = examMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Page<ExamVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(this::toBasicVO).collect(Collectors.toList()));
        return voPage;
    }

    public ExamVO getById(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) return null;
        ExamVO vo = toDetailVO(exam);

        long total = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>().eq(ExamSession::getExamId, id));
        long graded = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                .eq(ExamSession::getExamId, id)
                .eq(ExamSession::getStatus, "GRADED"));
        vo.setTotalSessions((int) total);
        vo.setGradedSessions((int) graded);

        List<ExamAssignment> assigns = assignmentMapper.selectList(
                new LambdaQueryWrapper<ExamAssignment>().eq(ExamAssignment::getExamId, id));
        vo.setAssignments(assigns.stream().map(a -> {
            AssignmentVO avo = new AssignmentVO();
            BeanUtils.copyProperties(a, avo);
            return avo;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Transactional
    public void create(ExamCreateDTO dto) {
        Exam exam = new Exam();
        BeanUtils.copyProperties(dto, exam);
        exam.setStatus(dto.getStatus() != null ? dto.getStatus() : "DRAFT");
        examMapper.insert(exam);

        if (dto.getAssignments() != null) {
            for (ExamCreateDTO.AssignmentItem item : dto.getAssignments()) {
                ExamAssignment assignment = new ExamAssignment();
                assignment.setExamId(exam.getId());
                assignment.setAssignType(item.getAssignType());
                assignment.setAssigneeId(item.getAssigneeId());
                assignmentMapper.insert(assignment);
            }
        }
    }

    @Transactional
    public void update(Long id, ExamCreateDTO dto) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) return;
        BeanUtils.copyProperties(dto, exam);
        exam.setId(id);
        examMapper.updateById(exam);

        // 先删后插分配
        assignmentMapper.delete(new LambdaQueryWrapper<ExamAssignment>().eq(ExamAssignment::getExamId, id));
        if (dto.getAssignments() != null) {
            for (ExamCreateDTO.AssignmentItem item : dto.getAssignments()) {
                ExamAssignment assignment = new ExamAssignment();
                assignment.setExamId(id);
                assignment.setAssignType(item.getAssignType());
                assignment.setAssigneeId(item.getAssigneeId());
                assignmentMapper.insert(assignment);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        assignmentMapper.delete(new LambdaQueryWrapper<ExamAssignment>().eq(ExamAssignment::getExamId, id));
        examMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setStatus(status);
        examMapper.updateById(exam);
    }

    @Transactional
    public void addAssignments(Long examId, AssignmentDTO dto) {
        if (dto.getUserIds() != null) {
            for (Long userId : dto.getUserIds()) {
                ExamAssignment a = new ExamAssignment();
                a.setExamId(examId); a.setAssignType("USER"); a.setAssigneeId(userId);
                assignmentMapper.insert(a);
            }
        }
        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                ExamAssignment a = new ExamAssignment();
                a.setExamId(examId); a.setAssignType("ROLE"); a.setAssigneeId(roleId);
                assignmentMapper.insert(a);
            }
        }
    }

    public List<AssignmentVO> getAssignments(Long examId) {
        List<ExamAssignment> list = assignmentMapper.selectList(
                new LambdaQueryWrapper<ExamAssignment>().eq(ExamAssignment::getExamId, examId));
        return list.stream().map(a -> {
            AssignmentVO vo = new AssignmentVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void removeAssignment(Long examId, Long assignmentId) {
        assignmentMapper.deleteById(assignmentId);
    }

    private ExamVO toBasicVO(Exam exam) {
        ExamVO vo = new ExamVO();
        BeanUtils.copyProperties(exam, vo);
        if (exam.getPaperId() != null) {
            Paper paper = paperMapper.selectById(exam.getPaperId());
            if (paper != null) vo.setPaperTitle(paper.getTitle());
        }
        return vo;
    }

    private ExamVO toDetailVO(Exam exam) {
        return toBasicVO(exam);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add services/paper-service/src/main/java/com/study/paper/exam/service/ExamService.java
git commit -m "feat(exam): add exam CRUD service"
```

---

### Task 6: ExamSessionService — 在线答题

**文件：**
- Create: `services/paper-service/src/main/java/com/study/paper/exam/service/ExamSessionService.java`

- [ ] **Step 1: 创建 ExamSessionService.java**

```java
package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.paper.exam.mapper.ExamAnswerMapper;
import com.study.paper.exam.mapper.ExamAssignmentMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamAnswer;
import com.study.paper.exam.model.entity.ExamAssignment;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.paper.mapper.PaperMapper;
import com.study.paper.paper.mapper.PaperSectionMapper;
import com.study.paper.paper.mapper.PaperQuestionMapper;
import com.study.paper.paper.model.entity.Paper;
import com.study.paper.paper.model.entity.PaperSection;
import com.study.paper.paper.model.entity.PaperQuestion;
import com.study.paper.common.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamSessionService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamAssignmentMapper assignmentMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private ExamAnswerMapper answerMapper;
    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperSectionMapper sectionMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取学生可见的考试列表
     */
    public List<ExamVO> getMyExams(Long userId, Long tenantId, String userType) {
        // 查询直接分配 + 角色匹配 + 考试码公开的考试
        List<Long> assignedExamIds = assignmentMapper.selectList(
                new LambdaQueryWrapper<ExamAssignment>()
                        .eq(ExamAssignment::getAssignType, "USER")
                        .eq(ExamAssignment::getAssigneeId, userId))
                .stream().map(ExamAssignment::getExamId).collect(Collectors.toList());

        List<Exam> allExams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getTenantId, tenantId)
                        .in(Exam::getStatus, "PUBLISHED", "IN_PROGRESS")
                        .orderByDesc(Exam::getCreatedAt));

        return allExams.stream()
                .filter(e -> isStudentAssigned(e, userId, assignedExamIds))
                .map(e -> {
                    ExamVO vo = new ExamVO();
                    BeanUtils.copyProperties(e, vo);
                    // 检查学生是否已有会话
                    ExamSession session = sessionMapper.selectOne(
                            new LambdaQueryWrapper<ExamSession>()
                                    .eq(ExamSession::getExamId, e.getId())
                                    .eq(ExamSession::getUserId, userId));
                    if (session != null) {
                        vo.setTotalSessions(1);
                        vo.setGradedSessions("GRADED".equals(session.getStatus()) ? 1 : 0);
                    }
                    return vo;
                }).collect(Collectors.toList());
    }

    private boolean isStudentAssigned(Exam exam, Long userId, List<Long> assignedExamIds) {
        if (assignedExamIds.contains(exam.getId())) return true;
        // EXAM_CODE 模式的考试所有人可见
        if (exam.getExamCode() != null && !exam.getExamCode().isEmpty()) return true;
        return false;
    }

    /**
     * 通过考试码加入考试
     */
    public ExamVO joinByCode(Long examId, String code, Long userId) {
        Exam exam = examMapper.selectOne(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getId, examId)
                        .eq(Exam::getExamCode, code));
        if (exam == null) throw new BusinessException("考试码无效");

        // 检查是否已有会话
        ExamSession existing = sessionMapper.selectOne(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .eq(ExamSession::getUserId, userId));
        if (existing != null) throw new BusinessException("已经参加过该考试");

        ExamVO vo = new ExamVO();
        BeanUtils.copyProperties(exam, vo);
        return vo;
    }

    /**
     * 开始考试：创建会话 + 试卷快照 + 预创建答题记录
     */
    @Transactional
    public ExamSessionVO startExam(Long examId, Long userId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!"PUBLISHED".equals(exam.getStatus()) && !"IN_PROGRESS".equals(exam.getStatus())) {
            throw new BusinessException("考试未发布");
        }

        // 时间校验
        if ("FIXED_WINDOW".equals(exam.getTimeMode()) || "BOTH".equals(exam.getTimeMode())) {
            LocalDateTime now = LocalDateTime.now();
            if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
                throw new BusinessException("考试还未开始");
            }
            if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
                throw new BusinessException("考试已结束");
            }
        }

        // 检查是否已有进行中的会话
        ExamSession existing = sessionMapper.selectOne(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .eq(ExamSession::getUserId, userId)
                        .in(ExamSession::getStatus, "IN_PROGRESS", "SUBMITTED", "GRADING"));
        if (existing != null) {
            if ("IN_PROGRESS".equals(existing.getStatus())) {
                // 返回已有会话
                return toSessionVO(existing, exam);
            }
            throw new BusinessException("已经参加过该考试");
        }

        // 加载试卷 + 题目快照
        Paper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null) throw new BusinessException("关联试卷不存在");

        List<PaperSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<PaperSection>().eq(PaperSection::getPaperId, paper.getId())
                        .orderByAsc(PaperSection::getSort));

        // 收集所有 questionId
        List<Long> questionIds = new ArrayList<>();
        Map<Long, Long> questionSectionMap = new HashMap<>();
        for (PaperSection section : sections) {
            List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, section.getId())
                            .orderByAsc(PaperQuestion::getSort));
            for (PaperQuestion pq : pqs) {
                questionIds.add(pq.getQuestionId());
                questionSectionMap.put(pq.getQuestionId(), section.getId());
            }
        }

        // 跨库查询题目内容
        List<Map<String, Object>> questions = questionIds.isEmpty()
                ? Collections.emptyList()
                : answerMapper.selectQuestionsByIds(questionIds);
        Map<Long, Map<String, Object>> questionMap = questions.stream()
                .collect(Collectors.toMap(q -> (Long) q.get("id"), q -> q));

        // 构建快照
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("paper", paper);
        List<Map<String, Object>> snapshotSections = new ArrayList<>();
        for (PaperSection section : sections) {
            Map<String, Object> secMap = new HashMap<>();
            secMap.put("id", section.getId());
            secMap.put("title", section.getTitle());
            secMap.put("sort", section.getSort());
            secMap.put("totalScore", section.getTotalScore());

            List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, section.getId())
                            .orderByAsc(PaperQuestion::getSort));
            List<Map<String, Object>> snapshotQuestions = new ArrayList<>();
            for (PaperQuestion pq : pqs) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("questionId", pq.getQuestionId());
                qMap.put("sort", pq.getSort());
                qMap.put("score", pq.getScore());
                Map<String, Object> qData = questionMap.get(pq.getQuestionId());
                if (qData != null) {
                    qMap.put("type", qData.get("type"));
                    qMap.put("contentJson", qData.get("contentJson"));
                    qMap.put("answerJson", qData.get("answerJson"));
                }
                snapshotQuestions.add(qMap);
            }
            secMap.put("questions", snapshotQuestions);
            snapshotSections.add(secMap);
        }
        snapshot.put("sections", snapshotSections);

        // 创建会话
        ExamSession session = new ExamSession();
        session.setExamId(examId);
        session.setUserId(userId);
        session.setPaperSnapshot(objectMapper.writeValueAsString(snapshot));
        session.setStartTime(LocalDateTime.now());
        session.setStatus("IN_PROGRESS");
        session.setTabSwitchCount(0);
        sessionMapper.insert(session);

        // 预创建答题记录
        for (PaperSection section : sections) {
            List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, section.getId()));
            for (PaperQuestion pq : pqs) {
                ExamAnswer answer = new ExamAnswer();
                answer.setSessionId(session.getId());
                answer.setSectionId(section.getId());
                answer.setQuestionId(pq.getQuestionId());
                Map<String, Object> qData = questionMap.get(pq.getQuestionId());
                answer.setQuestionType(qData != null ? (String) qData.get("type") : null);
                answer.setGradingStatus("UNGRADED");
                answerMapper.insert(answer);
            }
        }

        return toSessionVO(session, exam);
    }

    /**
     * 获取考试页面数据
     */
    public ExamSessionVO getSessionData(Long sessionId, Long userId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        Exam exam = examMapper.selectById(session.getExamId());
        return toSessionVO(session, exam);
    }

    /**
     * 保存单题答案
     */
    @Transactional
    public void saveAnswer(Long sessionId, Long userId, AnswerSubmitDTO dto) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException("考试已结束");
        }

        ExamAnswer answer = answerMapper.selectOne(
                new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getSessionId, sessionId)
                        .eq(ExamAnswer::getQuestionId, dto.getQuestionId()));
        if (answer != null) {
            answer.setAnswerJson(dto.getAnswerJson());
            answer.setAnsweredAt(LocalDateTime.now());
            answerMapper.updateById(answer);
        }
    }

    /**
     * 提交全卷 + 自动批改客观题
     */
    @Transactional
    public void submitExam(Long sessionId, Long userId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException("考试已提交");
        }

        // 解析快照获取题目答案
        List<Map<String, Object>> questions = extractQuestionsFromSnapshot(session.getPaperSnapshot());
        Map<Long, String> correctAnswers = new HashMap<>();
        Map<Long, String> questionTypes = new HashMap<>();
        for (Map<String, Object> q : questions) {
            Long qId = ((Number) q.get("questionId")).longValue();
            String answerJson = (String) q.get("answerJson");
            String type = (String) q.get("type");
            if (answerJson != null) {
                correctAnswers.put(qId, answerJson);
            }
            if (type != null) {
                questionTypes.put(qId, type);
            }
        }

        // 自动批改客观题
        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSessionId, sessionId));
        for (ExamAnswer answer : answers) {
            String type = answer.getQuestionType() != null ? answer.getQuestionType() : questionTypes.get(answer.getQuestionId());
            if (type == null) continue;

            if (isAutoGradable(type) && answer.getAnswerJson() != null && correctAnswers.containsKey(answer.getQuestionId())) {
                boolean correct = gradeAnswer(type, answer.getAnswerJson(), correctAnswers.get(answer.getQuestionId()));
                answer.setIsCorrect(correct ? 1 : 0);
                answer.setScore(correct ? getQuestionScore(questions, answer.getQuestionId()) : BigDecimal.ZERO);
                answer.setGradingStatus("AUTO_GRADED");
                answer.setGradedAt(LocalDateTime.now());
                answerMapper.updateById(answer);
            }
        }

        // 更新会话状态
        session.setStatus("SUBMITTED");
        session.setSubmittedAt(LocalDateTime.now());
        session.setDurationMinutes((int) ChronoUnit.MINUTES.between(session.getStartTime(), LocalDateTime.now()));
        sessionMapper.updateById(session);
    }

    /**
     * 心跳：同步切屏次数 + 检测阈值
     */
    @Transactional
    public void heartbeat(Long sessionId, Long userId, int tabSwitchCount) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) return;
        if (!"IN_PROGRESS".equals(session.getStatus())) return;

        session.setTabSwitchCount(tabSwitchCount);
        sessionMapper.updateById(session);

        Exam exam = examMapper.selectById(session.getExamId());
        if (exam.getMaxTabSwitches() != null && exam.getMaxTabSwitches() > 0
                && tabSwitchCount >= exam.getMaxTabSwitches()) {
            // 自动提交
            submitExam(sessionId, userId);
            // 标记为自动提交
            session.setStatus("AUTO_SUBMITTED");
            sessionMapper.updateById(session);
        }
    }

    /**
     * 查看成绩
     */
    public ExamSessionVO getResult(Long sessionId, Long userId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        if (!"GRADED".equals(session.getStatus())) {
            throw new BusinessException("成绩尚未发布");
        }

        Exam exam = examMapper.selectById(session.getExamId());
        ExamSessionVO vo = toSessionVO(session, exam);

        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSessionId, sessionId));
        List<ExamSessionVO.AnswerResultVO> resultList = new ArrayList<>();

        List<Map<String, Object>> snapshotQuestions = extractQuestionsFromSnapshot(session.getPaperSnapshot());
        Map<Long, Map<String, Object>> questionMap = snapshotQuestions.stream()
                .collect(Collectors.toMap(q -> ((Number) q.get("questionId")).longValue(), q -> q));

        for (ExamAnswer answer : answers) {
            ExamSessionVO.AnswerResultVO r = new ExamSessionVO.AnswerResultVO();
            r.setQuestionId(answer.getQuestionId());
            r.setQuestionType(answer.getQuestionType());
            r.setStudentAnswer(answer.getAnswerJson());
            r.setScore(answer.getScore());
            r.setGradingStatus(answer.getGradingStatus());
            r.setGraderComment(answer.getGraderComment());

            Map<String, Object> qData = questionMap.get(answer.getQuestionId());
            if (qData != null) {
                r.setCorrectAnswer((String) qData.get("answerJson"));
                r.setTotalScore(new BigDecimal(qData.get("score").toString()));
                // 解析 contentJson 获取题目文本
                String contentJson = (String) qData.get("contentJson");
                if (contentJson != null) {
                    try {
                        Map<String, Object> content = objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {});
                        r.setQuestionContent((String) content.getOrDefault("text", contentJson));
                    } catch (Exception e) {
                        r.setQuestionContent(contentJson);
                    }
                }
            }
            r.setIsCorrect(answer.getIsCorrect());
            resultList.add(r);
        }
        vo.setAnswers(resultList);
        return vo;
    }

    // --- 辅助方法 ---

    private ExamSessionVO toSessionVO(ExamSession session, Exam exam) {
        ExamSessionVO vo = new ExamSessionVO();
        BeanUtils.copyProperties(session, vo);
        vo.setUserId(session.getUserId());

        ExamSessionVO.ExamBasicVO basic = new ExamSessionVO.ExamBasicVO();
        basic.setTitle(exam.getTitle());
        basic.setDurationMinutes(exam.getDurationMinutes());
        basic.setMaxTabSwitches(exam.getMaxTabSwitches());

        if ("IN_PROGRESS".equals(session.getStatus()) && exam.getDurationMinutes() != null) {
            long elapsed = ChronoUnit.SECONDS.between(session.getStartTime(), LocalDateTime.now());
            int remaining = (int) (exam.getDurationMinutes() * 60 - elapsed);
            basic.setRemainingSeconds(Math.max(0, remaining));
        } else {
            basic.setRemainingSeconds(0);
        }
        vo.setExamInfo(basic);

        // 解析快照构建题目列表
        try {
            Map<String, Object> snapshot = objectMapper.readValue(session.getPaperSnapshot(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> snapSections = (List<Map<String, Object>>) snapshot.get("sections");
            if (snapSections != null) {
                List<ExamSessionVO.SectionVO> sectionVOs = new ArrayList<>();
                for (Map<String, Object> sec : snapSections) {
                    ExamSessionVO.SectionVO svo = new ExamSessionVO.SectionVO();
                    svo.setId(((Number) sec.get("id")).longValue());
                    svo.setTitle((String) sec.get("title"));
                    svo.setSort((Integer) sec.get("sort"));
                    svo.setTotalScore((Integer) sec.get("totalScore"));

                    List<Map<String, Object>> qs = (List<Map<String, Object>>) sec.get("questions");
                    if (qs != null) {
                        List<ExamSessionVO.QuestionVO> qVOs = new ArrayList<>();
                        for (Map<String, Object> q : qs) {
                            ExamSessionVO.QuestionVO qvo = new ExamSessionVO.QuestionVO();
                            qvo.setQuestionId(((Number) q.get("questionId")).longValue());
                            qvo.setSort((Integer) q.get("sort"));
                            qvo.setScore((Integer) q.get("score"));
                            qvo.setType((String) q.get("type"));
                            // 解析 contentJson
                            String contentJson = (String) q.get("contentJson");
                            if (contentJson != null) {
                                try {
                                    Map<String, Object> content = objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {});
                                    qvo.setContent(content);
                                } catch (Exception e) {
                                    Map<String, Object> fallback = new HashMap<>();
                                    fallback.put("text", contentJson);
                                    qvo.setContent(fallback);
                                }
                            }
                            qVOs.add(qvo);
                        }
                        svo.setQuestions(qVOs);
                    }
                    sectionVOs.add(svo);
                }
                vo.setSections(sectionVOs);
            }
        } catch (Exception e) {
            throw new BusinessException("试卷快照解析失败");
        }

        return vo;
    }

    private List<Map<String, Object>> extractQuestionsFromSnapshot(String snapshotJson) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            Map<String, Object> snapshot = objectMapper.readValue(snapshotJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> sections = (List<Map<String, Object>>) snapshot.get("sections");
            if (sections != null) {
                for (Map<String, Object> sec : sections) {
                    List<Map<String, Object>> qs = (List<Map<String, Object>>) sec.get("questions");
                    if (qs != null) result.addAll(qs);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private BigDecimal getQuestionScore(List<Map<String, Object>> questions, Long questionId) {
        return questions.stream()
                .filter(q -> ((Number) q.get("questionId")).longValue() == questionId)
                .findFirst()
                .map(q -> new BigDecimal(q.get("score").toString()))
                .orElse(BigDecimal.ZERO);
    }

    private boolean isAutoGradable(String type) {
        return Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_BLANK").contains(type);
    }

    private boolean gradeAnswer(String type, String studentAnswer, String correctAnswer) {
        if (studentAnswer == null || correctAnswer == null) return false;
        try {
            switch (type) {
                case "SINGLE_CHOICE":
                case "TRUE_FALSE":
                    return studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
                case "MULTIPLE_CHOICE":
                    // 比较 JSON 数组，顺序无关
                    Set<String> sSet = objectMapper.readValue(studentAnswer, new TypeReference<Set<String>>() {});
                    Set<String> cSet = objectMapper.readValue(correctAnswer, new TypeReference<Set<String>>() {});
                    return sSet.equals(cSet);
                case "FILL_BLANK":
                    return studentAnswer.trim().equals(correctAnswer.trim());
                default:
                    return false;
            }
        } catch (Exception e) {
            return studentAnswer.trim().equals(correctAnswer.trim());
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add services/paper-service/src/main/java/com/study/paper/exam/service/ExamSessionService.java
git commit -m "feat(exam): add exam session service with auto-grading"
```

---

### Task 7: GradingService — 人工批改 + 成绩发布

**文件：**
- Create: `services/paper-service/src/main/java/com/study/paper/exam/service/GradingService.java`

- [ ] **Step 1: 创建 GradingService.java**

```java
package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.paper.exam.mapper.ExamAnswerMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.ExamSessionVO;
import com.study.paper.exam.model.dto.ExamVO;
import com.study.paper.exam.model.dto.GradeSubmitDTO;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamAnswer;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.common.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GradingService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private ExamAnswerMapper answerMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 待批改考试列表（有已提交未批改完的考试）
     */
    public List<ExamVO> getGradingExams(Long tenantId) {
        List<Exam> exams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getTenantId, tenantId)
                        .orderByDesc(Exam::getCreatedAt));
        return exams.stream().map(e -> {
            ExamVO vo = new ExamVO();
            BeanUtils.copyProperties(e, vo);

            long total = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                    .eq(ExamSession::getExamId, e.getId())
                    .in(ExamSession::getStatus, "SUBMITTED", "GRADING", "GRADED"));
            long graded = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                    .eq(ExamSession::getExamId, e.getId())
                    .eq(ExamSession::getStatus, "GRADED"));

            vo.setTotalSessions((int) total);
            vo.setGradedSessions((int) graded);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取某考试下的待批改会话
     */
    public List<ExamSessionVO> getSessionsForGrading(Long examId, Long tenantId) {
        List<ExamSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .in(ExamSession::getStatus, "SUBMITTED", "GRADING")
                        .orderByAsc(ExamSession::getSubmittedAt));

        return sessions.stream().map(s -> {
            ExamSessionVO vo = new ExamSessionVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取某学生答卷详情（用于批改）
     */
    public ExamSessionVO getSessionForGrading(Long sessionId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException("会话不存在");

        Exam exam = examMapper.selectById(session.getExamId());
        ExamSessionVO vo = new ExamSessionVO();
        BeanUtils.copyProperties(session, vo);

        // 构建题目列表（从快照）
        List<Map<String, Object>> snapshotQuestions = extractQuestionsFromSnapshot(session.getPaperSnapshot());

        // 获取学生答案
        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSessionId, sessionId));

        List<ExamSessionVO.AnswerResultVO> resultList = new ArrayList<>();
        Map<Long, Map<String, Object>> questionMap = snapshotQuestions.stream()
                .collect(Collectors.toMap(q -> ((Number) q.get("questionId")).longValue(), q -> q));

        for (ExamAnswer answer : answers) {
            ExamSessionVO.AnswerResultVO r = new ExamSessionVO.AnswerResultVO();
            r.setQuestionId(answer.getQuestionId());
            r.setQuestionType(answer.getQuestionType());
            r.setStudentAnswer(answer.getAnswerJson());
            r.setScore(answer.getScore());
            r.setGradingStatus(answer.getGradingStatus());
            r.setGraderComment(answer.getGraderComment());

            Map<String, Object> qData = questionMap.get(answer.getQuestionId());
            if (qData != null) {
                r.setCorrectAnswer((String) qData.get("answerJson"));
                r.setTotalScore(new BigDecimal(qData.get("score").toString()));
                String contentJson = (String) qData.get("contentJson");
                if (contentJson != null) {
                    try {
                        Map<String, Object> content = objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {});
                        r.setQuestionContent((String) content.getOrDefault("text", contentJson));
                    } catch (Exception e) {
                        r.setQuestionContent(contentJson);
                    }
                }
            }
            resultList.add(r);
        }
        vo.setAnswers(resultList);
        return vo;
    }

    /**
     * 提交批改（逐题给分 + 评语）
     */
    @Transactional
    public void gradeSession(Long sessionId, Long graderId, GradeSubmitDTO dto) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException("会话不存在");

        for (GradeSubmitDTO.GradeItem item : dto.getGrades()) {
            ExamAnswer answer = answerMapper.selectOne(
                    new LambdaQueryWrapper<ExamAnswer>()
                            .eq(ExamAnswer::getSessionId, sessionId)
                            .eq(ExamAnswer::getQuestionId, item.getQuestionId()));
            if (answer != null && !"AUTO_GRADED".equals(answer.getGradingStatus())) {
                answer.setScore(item.getScore());
                answer.setGraderId(graderId);
                answer.setGraderComment(item.getComment());
                answer.setGradingStatus("MANUAL_GRADED");
                answer.setGradedAt(LocalDateTime.now());
                answerMapper.updateById(answer);
            }
        }

        // 更新会话状态为 GRADING（可能还有未批改的）
        if (!"GRADED".equals(session.getStatus())) {
            session.setStatus("GRADING");
            sessionMapper.updateById(session);
        }
    }

    /**
     * 发布成绩：检查是否全部批改完成，计算总分
     */
    @Transactional
    public void releaseGrades(Long sessionId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException("会话不存在");

        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSessionId, sessionId));

        // 检查是否有未批改的主观题
        boolean hasUngraded = answers.stream()
                .anyMatch(a -> "UNGRADED".equals(a.getGradingStatus()));
        if (hasUngraded) {
            throw new BusinessException("还有题目未批改，无法发布成绩");
        }

        // 计算总分
        BigDecimal total = answers.stream()
                .filter(a -> a.getScore() != null)
                .map(ExamAnswer::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        session.setTotalScore(total);
        session.setStatus("GRADED");
        sessionMapper.updateById(session);
    }

    private List<Map<String, Object>> extractQuestionsFromSnapshot(String snapshotJson) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            Map<String, Object> snapshot = objectMapper.readValue(snapshotJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> sections = (List<Map<String, Object>>) snapshot.get("sections");
            if (sections != null) {
                for (Map<String, Object> sec : sections) {
                    List<Map<String, Object>> qs = (List<Map<String, Object>>) sec.get("questions");
                    if (qs != null) result.addAll(qs);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add services/paper-service/src/main/java/com/study/paper/exam/service/GradingService.java
git commit -m "feat(exam): add grading service"
```

---

### Task 8: ExamController — 教师端考试管理

**文件：**
- Create: `services/paper-service/src/main/java/com/study/paper/exam/controller/ExamController.java`
- Create: `services/paper-service/src/main/java/com/study/paper/exam/controller/GradingController.java`

- [ ] **Step 1: 创建 ExamController.java**

```java
package com.study.paper.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @GetMapping("/page")
    public Result<Page<ExamVO>> page(@Valid ExamQueryDTO query) {
        return Result.ok(examService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ExamVO> getById(@PathVariable Long id) {
        return Result.ok(examService.getById(id));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody ExamCreateDTO dto) {
        examService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ExamCreateDTO dto) {
        examService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        examService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        examService.updateStatus(id, status);
        return Result.ok();
    }

    @PostMapping("/{id}/assignments")
    public Result<?> addAssignments(@PathVariable Long id, @Valid @RequestBody AssignmentDTO dto) {
        examService.addAssignments(id, dto);
        return Result.ok();
    }

    @GetMapping("/{id}/assignments")
    public Result<?> getAssignments(@PathVariable Long id) {
        return Result.ok(examService.getAssignments(id));
    }

    @DeleteMapping("/{examId}/assignments/{id}")
    public Result<?> removeAssignment(@PathVariable Long examId, @PathVariable Long id) {
        examService.removeAssignment(examId, id);
        return Result.ok();
    }
}
```

- [ ] **Step 2: 创建 GradingController.java**

```java
package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.service.GradingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grading")
public class GradingController {

    @Autowired
    private GradingService gradingService;

    @GetMapping("/exams")
    public Result<?> getGradingExams() {
        // 从请求头获取 tenantId，简化处理先传 0
        return Result.ok(gradingService.getGradingExams(0L));
    }

    @GetMapping("/exams/{examId}/sessions")
    public Result<?> getSessionsForGrading(@PathVariable Long examId) {
        return Result.ok(gradingService.getSessionsForGrading(examId, 0L));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<?> getSessionForGrading(@PathVariable Long sessionId) {
        return Result.ok(gradingService.getSessionForGrading(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/grade")
    public Result<?> gradeSession(@PathVariable Long sessionId,
                                   @Valid @RequestBody GradeSubmitDTO dto) {
        gradingService.gradeSession(sessionId, 1L, dto);
        return Result.ok();
    }

    @PostMapping("/sessions/{sessionId}/release")
    public Result<?> releaseGrades(@PathVariable Long sessionId) {
        gradingService.releaseGrades(sessionId);
        return Result.ok();
    }
}
```

注意：`GradingController` 中的 `graderId` 和 `tenantId` 在生产环境中应从请求头 `X-User-Id`、`X-Tenant-Id` 获取（由网关透传），简化实现先传固定值。

- [ ] **Step 3: 提交**

```bash
git add services/paper-service/src/main/java/com/study/paper/exam/controller/ExamController.java services/paper-service/src/main/java/com/study/paper/exam/controller/GradingController.java
git commit -m "feat(exam): add exam and grading controllers"
```

---

### Task 9: MyExamController — 学生端 + 心跳

**文件：**
- Create: `services/paper-service/src/main/java/com/study/paper/exam/controller/MyExamController.java`

- [ ] **Step 1: 创建 MyExamController.java**

```java
package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.service.ExamSessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my-exams")
public class MyExamController {

    @Autowired
    private ExamSessionService sessionService;

    @GetMapping
    public Result<?> getMyExams(@RequestHeader("X-User-Id") Long userId,
                                @RequestHeader("X-Tenant-Id") Long tenantId) {
        return Result.ok(sessionService.getMyExams(userId, tenantId, null));
    }

    @PostMapping("/{examId}/join")
    public Result<?> joinByCode(@PathVariable Long examId, @RequestParam String code,
                                @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(sessionService.joinByCode(examId, code, userId));
    }

    @PostMapping("/{examId}/start")
    public Result<?> startExam(@PathVariable Long examId,
                               @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(sessionService.startExam(examId, userId));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<?> getSessionData(@PathVariable Long sessionId,
                                    @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(sessionService.getSessionData(sessionId, userId));
    }

    @PostMapping("/sessions/{sessionId}/answer")
    public Result<?> saveAnswer(@PathVariable Long sessionId,
                                @RequestHeader("X-User-Id") Long userId,
                                @Valid @RequestBody AnswerSubmitDTO dto) {
        sessionService.saveAnswer(sessionId, userId, dto);
        return Result.ok();
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public Result<?> submitExam(@PathVariable Long sessionId,
                                @RequestHeader("X-User-Id") Long userId) {
        sessionService.submitExam(sessionId, userId);
        return Result.ok();
    }

    @GetMapping("/sessions/{sessionId}/result")
    public Result<?> getResult(@PathVariable Long sessionId,
                               @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(sessionService.getResult(sessionId, userId));
    }

    @PostMapping("/sessions/{sessionId}/heartbeat")
    public Result<?> heartbeat(@PathVariable Long sessionId,
                               @RequestHeader("X-User-Id") Long userId,
                               @RequestParam int tabSwitchCount) {
        sessionService.heartbeat(sessionId, userId, tabSwitchCount);
        return Result.ok();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add services/paper-service/src/main/java/com/study/paper/exam/controller/MyExamController.java
git commit -m "feat(exam): add student exam controller"
```

---

### Task 10: 后端编译验证

- [ ] **Step 1: 编译 paper-service**

```bash
mvn compile -pl services/paper-service -am -q 2>&1 | tail -20
```

预期：BUILD SUCCESS

- [ ] **Step 2: 启动所有服务验证**

重启 paper-service 和 gateway：

```bash
taskkill /F /IM "java.exe" 2>/dev/null; sleep 2
cd "D:/workspace/study-system/services/paper-service" && mvn spring-boot:run 2>&1 &
cd "D:/workspace/study-system/services/gateway" && mvn spring-boot:run 2>&1 &
```

- [ ] **Step 3: 测试 API**

```bash
# 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# 创建考试
curl -s -X POST http://localhost:8080/api/exams \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  --data-binary '{"paperId":1,"title":"Test Exam","timeMode":"FLEXIBLE","durationMinutes":60,"assignments":[{"assignType":"EXAM_CODE","assigneeId":null}]}'

# 列表
curl -s "http://localhost:8080/api/exams/page" -H "Authorization: Bearer $TOKEN"
```

预期：考试创建成功，列表返回正确。

---

### Task 11: 前端 API 模块 + 路由

**文件：**
- Create: `frontend/study-front/src/api/exam.ts`
- Modify: `frontend/study-front/src/router/index.ts`
- Modify: `frontend/study-front/src/layouts/MainLayout.vue`（添加菜单）

- [ ] **Step 1: 创建 exam.ts**

```typescript
import request from './request'

export function getExamPage(params: any) {
  return request.get('/exams/page', { params })
}

export function getExamById(id: number) {
  return request.get(`/exams/${id}`)
}

export function createExam(data: any) {
  return request.post('/exams', data)
}

export function updateExam(id: number, data: any) {
  return request.put(`/exams/${id}`, data)
}

export function deleteExam(id: number) {
  return request.delete(`/exams/${id}`)
}

export function updateExamStatus(id: number, status: string) {
  return request.put(`/exams/${id}/status?status=${status}`)
}

export function addAssignments(examId: number, data: any) {
  return request.post(`/exams/${examId}/assignments`, data)
}

export function getAssignments(examId: number) {
  return request.get(`/exams/${examId}/assignments`)
}

export function removeAssignment(examId: number, id: number) {
  return request.delete(`/exams/${examId}/assignments/${id}`)
}

export function getMyExams() {
  return request.get('/my-exams')
}

export function joinExamByCode(examId: number, code: string) {
  return request.post(`/my-exams/${examId}/join?code=${code}`)
}

export function startExam(examId: number) {
  return request.post(`/my-exams/${examId}/start`)
}

export function getSessionData(sessionId: number) {
  return request.get(`/my-exams/sessions/${sessionId}`)
}

export function saveAnswer(sessionId: number, data: any) {
  return request.post(`/my-exams/sessions/${sessionId}/answer`, data)
}

export function submitExam(sessionId: number) {
  return request.post(`/my-exams/sessions/${sessionId}/submit`)
}

export function getExamResult(sessionId: number) {
  return request.get(`/my-exams/sessions/${sessionId}/result`)
}

export function heartbeat(sessionId: number, tabSwitchCount: number) {
  return request.post(`/my-exams/sessions/${sessionId}/heartbeat?tabSwitchCount=${tabSwitchCount}`)
}

export function getGradingExams() {
  return request.get('/grading/exams')
}

export function getGradingSessions(examId: number) {
  return request.get(`/grading/exams/${examId}/sessions`)
}

export function getSessionForGrading(sessionId: number) {
  return request.get(`/grading/sessions/${sessionId}`)
}

export function gradeSession(sessionId: number, data: any) {
  return request.post(`/grading/sessions/${sessionId}/grade`, data)
}

export function releaseGrades(sessionId: number) {
  return request.post(`/grading/sessions/${sessionId}/release`)
}
```

- [ ] **Step 2: 更新路由**

修改 `frontend/study-front/src/router/index.ts`，在 paper 子菜单下方添加：

```typescript
        {
          path: 'exams',
          name: 'Exams',
          component: () => import('@/views/exam/ExamListView.vue'),
          meta: { title: '考试安排' },
        },
        {
          path: 'exams/create',
          name: 'ExamCreate',
          component: () => import('@/views/exam/ExamCreateView.vue'),
          meta: { title: '创建考试' },
        },
        {
          path: 'exams/:id',
          name: 'ExamDetail',
          component: () => import('@/views/exam/ExamDetailView.vue'),
          meta: { title: '考试详情' },
        },
        {
          path: 'exams/:id/edit',
          name: 'ExamEdit',
          component: () => import('@/views/exam/ExamCreateView.vue'),
          meta: { title: '编辑考试' },
        },
        {
          path: 'my-exams',
          name: 'MyExams',
          component: () => import('@/views/exam/MyExamsView.vue'),
          meta: { title: '我的考试' },
        },
        {
          path: 'exam/session/:sessionId',
          name: 'ExamSession',
          component: () => import('@/views/exam/ExamSessionView.vue'),
          meta: { title: '在线答题' },
        },
        {
          path: 'exam/result/:sessionId',
          name: 'ExamResult',
          component: () => import('@/views/exam/ExamResultView.vue'),
          meta: { title: '考试成绩' },
        },
        {
          path: 'grading/exams',
          name: 'GradingExams',
          component: () => import('@/views/exam/GradingExamListView.vue'),
          meta: { title: '批改管理' },
        },
        {
          path: 'grading/exams/:examId/sessions',
          name: 'GradingSessions',
          component: () => import('@/views/exam/GradingSessionView.vue'),
          meta: { title: '批改答卷' },
        },
```

- [ ] **Step 3: 更新菜单**

修改 `frontend/study-front/src/layouts/MainLayout.vue`，在试卷管理的子菜单中添加：

```html
          <el-menu-item index="/exams">考试安排</el-menu-item>
          <el-menu-item index="/my-exams">我的考试</el-menu-item>
          <el-menu-item index="/grading/exams">批改管理</el-menu-item>
```

放在 `</el-sub-menu>` 关闭标签之前。

- [ ] **Step 4: 提交**

```bash
git add frontend/study-front/src/api/exam.ts frontend/study-front/src/router/index.ts frontend/study-front/src/layouts/MainLayout.vue
git commit -m "feat(exam): add frontend API module and routes"
```

---

### Task 12: 前端页面 — 考试管理（教师端）

**文件：**
- Create: `frontend/study-front/src/views/exam/ExamListView.vue`
- Create: `frontend/study-front/src/views/exam/ExamCreateView.vue`
- Create: `frontend/study-front/src/views/exam/ExamDetailView.vue`

- [ ] **Step 1: 创建 ExamListView.vue**

```vue
<template>
  <div>
    <el-row justify="space-between" align="middle">
      <h3>考试安排</h3>
      <el-button type="primary" @click="router.push('/exams/create')">创建考试</el-button>
    </el-row>

    <el-card style="margin-top: 16px">
      <el-form :inline="true" :model="query">
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="搜索标题" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已结束" value="FINISHED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchExams">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="examList" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="title" label="考试名称" min-width="180" />
      <el-table-column prop="paperTitle" label="关联试卷" min-width="150" />
      <el-table-column label="时间模式" width="100">
        <template #default="{ row }">
          {{ { FIXED_WINDOW: '固定时段', FLEXIBLE: '灵活时长', BOTH: '两者' }[row.timeMode] || row.timeMode }}
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="160">
        <template #default="{ row }">{{ row.startTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="作答时长" width="80">
        <template #default="{ row }">{{ row.durationMinutes || '-' }}分钟</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="router.push(`/exams/${row.id}`)">详情</el-button>
          <el-button size="small" @click="router.push(`/exams/${row.id}/edit`)" :disabled="row.status!=='DRAFT'">编辑</el-button>
          <el-button size="small" :type="row.status==='PUBLISHED'?'warning':'success'"
            @click="toggleStatus(row)" :disabled="row.status==='FINISHED'">
            {{ row.status==='PUBLISHED'?'结束':'发布' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)" :disabled="row.status!=='DRAFT'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page" v-model:page-size="query.size"
      :total="total" layout="total, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end" @change="fetchExams" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getExamPage, deleteExam, updateExamStatus } from '@/api/exam'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const examList = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, title: undefined, status: undefined })

onMounted(() => fetchExams())

async function fetchExams() {
  loading.value = true
  try {
    const res: any = await getExamPage(query)
    examList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { Object.assign(query, { page: 1, title: undefined, status: undefined }); fetchExams() }

async function toggleStatus(row: any) {
  const newStatus = row.status === 'PUBLISHED' ? 'FINISHED' : 'PUBLISHED'
  await updateExamStatus(row.id, newStatus)
  ElMessage.success(newStatus === 'PUBLISHED' ? '已发布' : '已结束')
  fetchExams()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该考试安排吗？', '提示')
  await deleteExam(id)
  ElMessage.success('删除成功')
  fetchExams()
}

function statusType(s: string) {
  const map: Record<string, string> = { DRAFT: 'info', PUBLISHED: 'success', IN_PROGRESS: 'warning', FINISHED: '' }
  return map[s] || 'info'
}
function statusLabel(s: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return map[s] || s
}
</script>
```

- [ ] **Step 2: 创建 ExamCreateView.vue**

```vue
<template>
  <div v-loading="loading">
    <h3>{{ isEdit ? '编辑考试' : '创建考试' }}</h3>

    <el-card style="margin-top: 16px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="考试名称" required>
          <el-input v-model="form.title" placeholder="如：期中数学考试" />
        </el-form-item>
        <el-form-item label="关联试卷" required>
          <el-select v-model="form.paperId" placeholder="选择试卷" style="width: 100%">
            <el-option v-for="p in papers" :key="p.id" :label="p.title" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="时间模式" required>
          <el-radio-group v-model="form.timeMode">
            <el-radio value="FIXED_WINDOW">固定时段</el-radio>
            <el-radio value="FLEXIBLE">灵活时长</el-radio>
            <el-radio value="BOTH">两者结合</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="20" v-if="form.timeMode !== 'FLEXIBLE'">
          <el-col :span="12">
            <el-form-item label="开始时间" required>
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" required>
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="作答时长(分钟)" v-if="form.timeMode !== 'FIXED_WINDOW'">
          <el-input-number v-model="form.durationMinutes" :min="0" style="width: 200px" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>分配考生</span></template>
      <el-radio-group v-model="assignMode" style="margin-bottom: 12px">
        <el-radio value="USER">按用户</el-radio>
        <el-radio value="ROLE">按角色</el-radio>
        <el-radio value="EXAM_CODE">考试码</el-radio>
      </el-radio-group>

      <div v-if="assignMode === 'USER'">
        <el-button size="small" type="primary" @click="showUserSelector">选择用户</el-button>
        <el-tag v-for="u in selectedUsers" :key="u.id" closable @close="removeUser(u)" style="margin-left: 8px">{{ u.realName || u.username }}</el-tag>
      </div>
      <div v-if="assignMode === 'ROLE'">
        <el-select v-model="selectedRoles" multiple placeholder="选择角色" style="width: 300px">
          <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
      </div>
      <div v-if="assignMode === 'EXAM_CODE'">
        <el-input v-model="form.examCode" placeholder="输入考试码，留空自动生成" style="width: 300px" />
      </div>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>防作弊设置</span></template>
      <el-form label-width="120px">
        <el-form-item label="切屏检测">
          <el-switch v-model="antiCheatEnabled" />
        </el-form-item>
        <el-form-item label="允许切屏次数" v-if="antiCheatEnabled">
          <el-input-number v-model="form.maxTabSwitches" :min="1" :max="10" />
        </el-form-item>
      </el-form>
    </el-card>

    <div style="margin-top: 16px; text-align: center">
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      <el-button @click="router.back()">取消</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createExam, updateExam, getExamById } from '@/api/exam'
import { getPaperPage } from '@/api/paper'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const isEdit = ref(false)
const loading = ref(false)
const saving = ref(false)
const papers = ref<any[]>([])
const roles = ref<any[]>([])
const selectedUsers = ref<any[]>([])
const selectedRoles = ref<number[]>([])
const assignMode = ref('EXAM_CODE')
const antiCheatEnabled = ref(false)

const form = reactive({
  paperId: undefined as number | undefined,
  title: '',
  description: '',
  examCode: '',
  timeMode: 'FLEXIBLE',
  startTime: undefined,
  endTime: undefined,
  durationMinutes: 60,
  maxTabSwitches: 3,
  antiCheatConfig: '',
})

onMounted(async () => {
  const res: any = await getPaperPage({ page: 1, size: 999 })
  papers.value = res.data.records

  if (route.params.id) {
    isEdit.value = true
    loading.value = true
    try {
      const res: any = await getExamById(Number(route.params.id))
      const d = res.data
      Object.assign(form, { title: d.title, paperId: d.paperId, description: d.description || '',
        examCode: d.examCode || '', timeMode: d.timeMode, startTime: d.startTime,
        endTime: d.endTime, durationMinutes: d.durationMinutes, maxTabSwitches: d.maxTabSwitches || 3 })
    } finally { loading.value = false }
  }
})

async function handleSave() {
  if (!form.title || !form.paperId) { ElMessage.warning('请填写名称和关联试卷'); return }
  saving.value = true
  try {
    const payload: any = { ...form, maxTabSwitches: antiCheatEnabled.value ? form.maxTabSwitches : 0 }
    payload.assignments = []
    if (assignMode.value === 'USER') {
      for (const u of selectedUsers.value) payload.assignments.push({ assignType: 'USER', assigneeId: u.id })
    } else if (assignMode.value === 'ROLE') {
      for (const r of selectedRoles.value) payload.assignments.push({ assignType: 'ROLE', assigneeId: r })
    }
    if (isEdit.value) {
      await updateExam(Number(route.params.id), payload)
      ElMessage.success('更新成功')
    } else {
      await createExam(payload)
      ElMessage.success('创建成功')
    }
    router.push('/exams')
  } catch {} finally { saving.value = false }
}

function showUserSelector() { /* TODO: 用户选择对话框 */ ElMessage.info('请通过角色或考试码分配') }
function removeUser(u: any) { selectedUsers.value = selectedUsers.value.filter((x: any) => x.id !== u.id) }
</script>
```

- [ ] **Step 3: 创建 ExamDetailView.vue**

```vue
<template>
  <div v-loading="loading">
    <el-row justify="space-between" align="middle">
      <h3>考试详情</h3>
      <el-button @click="router.back()">返回</el-button>
    </el-row>

    <el-card style="margin-top: 16px" v-if="exam">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="考试名称" :span="2">{{ exam.title }}</el-descriptions-item>
        <el-descriptions-item label="关联试卷">{{ exam.paperTitle }}</el-descriptions-item>
        <el-descriptions-item label="时间模式">{{ {FIXED_WINDOW:'固定时段',FLEXIBLE:'灵活时长',BOTH:'两者'}[exam.timeMode] }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ exam.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ exam.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="作答时长">{{ exam.durationMinutes || '-' }}分钟</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="exam.status==='PUBLISHED'?'success':'info'" size="small">{{ exam.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="批改进度">{{ exam.gradedSessions }}/{{ exam.totalSessions }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ exam.description || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>分配列表</span></template>
      <el-table :data="assignments" border stripe>
        <el-table-column prop="assignType" label="分配方式" width="120">
          <template #default="{row}">{{ {USER:'用户',ROLE:'角色',EXAM_CODE:'考试码'}[row.assignType] }}</template>
        </el-table-column>
        <el-table-column prop="assigneeId" label="分配对象ID" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getExamById, getAssignments } from '@/api/exam'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const exam = ref<any>(null)
const assignments = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const [examRes, assignRes]: any = await Promise.all([getExamById(id), getAssignments(id)])
    exam.value = examRes.data
    assignments.value = assignRes.data || []
  } catch { ElMessage.error('加载失败')
  } finally { loading.value = false }
})
</script>
```

- [ ] **Step 4: 提交**

```bash
git add frontend/study-front/src/views/exam/ExamListView.vue frontend/study-front/src/views/exam/ExamCreateView.vue frontend/study-front/src/views/exam/ExamDetailView.vue
git commit -m "feat(exam): add exam management pages"
```

---

### Task 13: 前端页面 — 在线答题（学生端核心）

**文件：**
- Create: `frontend/study-front/src/views/exam/MyExamsView.vue`
- Create: `frontend/study-front/src/views/exam/components/ExamTimer.vue`
- Create: `frontend/study-front/src/views/exam/components/QuestionNavigator.vue`
- Create: `frontend/study-front/src/views/exam/components/QuestionRenderer.vue`
- Create: `frontend/study-front/src/views/exam/ExamSessionView.vue`

- [ ] **Step 1: 创建 MyExamsView.vue**

```vue
<template>
  <div>
    <h3>我的考试</h3>

    <el-card style="margin-top: 16px">
      <template #header><span>待考</span></template>
      <el-empty v-if="pendingExams.length === 0" description="暂无待考考试" />
      <el-table v-else :data="pendingExams" border stripe>
        <el-table-column prop="title" label="考试名称" min-width="180" />
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="100" />
        <el-table-column label="时间范围" min-width="200">
          <template #default="{row}">{{ row.startTime || '-' }} ~ {{ row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{row}">
            <el-tag size="small">待参加</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button type="primary" size="small" @click="handleStart(row)">开始考试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>已考</span></template>
      <el-empty v-if="completedExams.length === 0" description="暂无已完成考试" />
      <el-table v-else :data="completedExams" border stripe>
        <el-table-column prop="title" label="考试名称" min-width="180" />
        <el-table-column prop="submittedAt" label="提交时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.sessionStatus==='GRADED'?'success':''" size="small">
              {{ row.sessionStatus==='GRADED'?'已出分':row.sessionStatus==='SUBMITTED'?'待批改':'批改中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button size="small" :disabled="row.sessionStatus!=='GRADED'" @click="router.push(`/exam/result/${row.sessionId}`)">查看成绩</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyExams, startExam } from '@/api/exam'
import { ElMessage } from 'element-plus'

const router = useRouter()
const exams = ref<any[]>([])

const pendingExams = computed(() => exams.value.filter((e: any) => !e.totalSessions))
const completedExams = computed(() => exams.value.filter((e: any) => e.totalSessions))

onMounted(() => fetchMyExams())

async function fetchMyExams() {
  const res: any = await getMyExams()
  exams.value = res.data || []
}

async function handleStart(row: any) {
  try {
    const res: any = await startExam(row.id)
    router.push(`/exam/session/${res.data.id}`)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '开始考试失败')
  }
}
</script>
```

- [ ] **Step 2: 创建 ExamTimer.vue**

```vue
<template>
  <div :class="['exam-timer', { warning: remaining <= 300 }]">
    <el-icon><Timer /></el-icon>
    <span class="timer-text">{{ formatted }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Timer } from '@element-plus/icons-vue'

const props = defineProps<{ remainingSeconds: number }>()
const emit = defineEmits<{ expired: [] }>()

const remaining = ref(props.remainingSeconds)
let timer: number | null = null

onMounted(() => {
  timer = window.setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) { clearInterval(timer!); emit('expired') }
  }, 1000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })

const formatted = computed(() => {
  const h = Math.floor(remaining.value / 3600)
  const m = Math.floor((remaining.value % 3600) / 60)
  const s = remaining.value % 60
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
})
</script>

<style scoped>
.exam-timer { font-size: 24px; font-weight: bold; color: #409EFF; }
.exam-timer.warning { color: #E6A23C; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.5; } }
.timer-text { margin-left: 8px; }
</style>
```

- [ ] **Step 3: 创建 QuestionNavigator.vue**

```vue
<template>
  <div class="navigator">
    <div v-for="section in sections" :key="section.id" class="nav-section">
      <div class="nav-section-title">{{ section.title }}</div>
      <div class="nav-questions">
        <div v-for="q in section.questions" :key="q.questionId"
          :class="['nav-item', { active: currentId === q.questionId, answered: answeredIds.has(q.questionId) }]"
          @click="$emit('select', q.questionId)">
          {{ q.sort != null ? q.sort + 1 : q.questionId }}
        </div>
      </div>
    </div>
    <div class="nav-legend">
      <span><span class="dot answered"></span> 已答</span>
      <span><span class="dot"></span> 未答</span>
      <span><span class="dot active"></span> 当前</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ sections: any[]; currentId: number; answeredIds: Set<number> }>()
defineEmits<{ select: [id: number] }>()
</script>

<style scoped>
.navigator { width: 180px; padding: 12px; }
.nav-section { margin-bottom: 16px; }
.nav-section-title { font-size: 13px; font-weight: bold; margin-bottom: 8px; color: #606266; }
.nav-questions { display: flex; flex-wrap: wrap; gap: 6px; }
.nav-item { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border: 1px solid #dcdfe6; border-radius: 4px; cursor: pointer; font-size: 13px; }
.nav-item.answered { background: #409EFF; color: #fff; border-color: #409EFF; }
.nav-item.active { border-color: #E6A23C; border-width: 2px; font-weight: bold; }
.nav-legend { display: flex; gap: 12px; margin-top: 16px; font-size: 12px; color: #909399; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; border: 1px solid #dcdfe6; margin-right: 4px; }
.dot.answered { background: #409EFF; border-color: #409EFF; }
.dot.active { border-color: #E6A23C; border-width: 2px; }
</style>
```

- [ ] **Step 4: 创建 QuestionRenderer.vue**

```vue
<template>
  <div class="question-renderer">
    <div class="question-text" v-html="textContent"></div>

    <!-- 单选题 -->
    <el-radio-group v-if="type === 'SINGLE_CHOICE'" v-model="selectedValue" @change="onChange">
      <el-radio v-for="(opt, k) in options" :key="k" :value="k" class="option-item">{{ k }}. {{ opt }}</el-radio>
    </el-radio-group>

    <!-- 多选题 -->
    <el-checkbox-group v-else-if="type === 'MULTIPLE_CHOICE'" v-model="selectedArray" @change="onChange">
      <el-checkbox v-for="(opt, k) in options" :key="k" :value="k" class="option-item">{{ k }}. {{ opt }}</el-checkbox>
    </el-checkbox-group>

    <!-- 判断题 -->
    <el-radio-group v-else-if="type === 'TRUE_FALSE'" v-model="selectedValue" @change="onChange">
      <el-radio value="true" class="option-item">正确</el-radio>
      <el-radio value="false" class="option-item">错误</el-radio>
    </el-radio-group>

    <!-- 填空题 -->
    <el-input v-else-if="type === 'FILL_BLANK'" v-model="textValue" @change="onChange"
      placeholder="请输入答案" style="width: 300px" />

    <!-- 简答/论述题 -->
    <el-input v-else-if="type === 'SHORT_ANSWER' || type === 'ESSAY'" v-model="textValue" @change="onChange"
      type="textarea" :rows="6" placeholder="请输入答案" />

    <!-- 组合题 -->
    <div v-else-if="type === 'COMPOSITE'">
      <el-alert type="info" :closable="false" show-icon title="组合题" description="请按子题依次作答" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{ type: string; content: any; answer?: string }>()
const emit = defineEmits<{ answer: [value: string] }>()

const selectedValue = ref('')
const selectedArray = ref<string[]>([])
const textValue = ref('')

watch(() => props.answer, (v) => {
  if (!v) return
  if (props.type === 'SINGLE_CHOICE' || props.type === 'TRUE_FALSE') selectedValue.value = v
  else if (props.type === 'MULTIPLE_CHOICE') {
    try { selectedArray.value = JSON.parse(v) } catch { selectedArray.value = [] }
  } else textValue.value = v
}, { immediate: true })

const textContent = computed(() => props.content?.text || props.content?.passage || '')
const options = computed(() => props.content?.options || {})

function onChange() {
  let val = ''
  if (props.type === 'SINGLE_CHOICE' || props.type === 'TRUE_FALSE') val = selectedValue.value
  else if (props.type === 'MULTIPLE_CHOICE') val = JSON.stringify(selectedArray.value)
  else val = textValue.value
  emit('answer', val)
}
</script>

<style scoped>
.question-renderer { padding: 16px 0; }
.question-text { font-size: 15px; line-height: 1.8; margin-bottom: 16px; white-space: pre-wrap; }
.option-item { display: flex; margin: 8px 0; }
</style>
```

- [ ] **Step 5: 创建 ExamSessionView.vue（核心答题页）**

```vue
<template>
  <div class="exam-session" v-loading="loading">
    <!-- 顶部栏 -->
    <div class="exam-header">
      <h3>{{ sessionData?.examInfo?.title || '在线答题' }}</h3>
      <div class="header-right">
        <ExamTimer v-if="sessionData?.examInfo?.remainingSeconds > 0"
          :remaining-seconds="sessionData.examInfo.remainingSeconds" @expired="handleSubmit" />
        <el-button type="danger" @click="handleSubmit" :loading="submitting">交卷</el-button>
      </div>
    </div>

    <div class="exam-body" v-if="sessionData">
      <!-- 左侧导航 -->
      <QuestionNavigator
        :sections="sessionData.sections"
        :current-id="currentQuestionId"
        :answered-ids="answeredIds"
        @select="navigateTo" />

      <!-- 右侧答题区 -->
      <div class="question-area">
        <div v-for="section in sessionData.sections" :key="section.id" v-show="currentSection?.id === section.id">
          <h4>{{ section.title }}</h4>
          <div v-for="q in section.questions" :key="q.questionId" v-show="q.questionId === currentQuestionId">
            <div class="question-meta">本题 {{ q.score }} 分</div>
            <QuestionRenderer :type="q.type" :content="q.content" :answer="getAnswer(q.questionId)"
              @answer="(val: string) => saveAnswer(q, val)" />
          </div>
        </div>

        <div class="question-nav-buttons">
          <el-button @click="prevQuestion" :disabled="isFirst">上一题</el-button>
          <el-button type="primary" @click="nextQuestion" :disabled="isLast">下一题</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getSessionData, saveAnswer, submitExam, heartbeat } from '@/api/exam'
import { ElMessage, ElMessageBox } from 'element-plus'
import ExamTimer from './components/ExamTimer.vue'
import QuestionNavigator from './components/QuestionNavigator.vue'
import QuestionRenderer from './components/QuestionRenderer.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const sessionData = ref<any>(null)
const currentQuestionId = ref(0)
const answers = ref<Record<number, string>>({})

const allQuestions = computed(() => {
  const qs: any[] = []
  for (const s of sessionData.value?.sections || []) {
    for (const q of s.questions || []) qs.push(q)
  }
  return qs
})

const currentSection = computed(() => {
  for (const s of sessionData.value?.sections || []) {
    if (s.questions?.find((q: any) => q.questionId === currentQuestionId.value)) return s
  }
  return null
})

const answeredIds = computed(() => new Set(
  Object.entries(answers.value).filter(([, v]) => v).map(([k]) => Number(k))
))

const currentIdx = computed(() => allQuestions.value.findIndex(q => q.questionId === currentQuestionId.value))
const isFirst = computed(() => currentIdx.value <= 0)
const isLast = computed(() => currentIdx.value >= allQuestions.value.length - 1)

let heartbeatTimer: number | null = null
let tabSwitchCount = 0

onMounted(async () => {
  loading.value = true
  try {
    const sessionId = Number(route.params.sessionId)
    const res: any = await getSessionData(sessionId)
    sessionData.value = res.data
    if (sessionData.value?.sections?.[0]?.questions?.[0]) {
      currentQuestionId.value = sessionData.value.sections[0].questions[0].questionId
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    heartbeatTimer = window.setInterval(sendHeartbeat, 30000)
  } catch {
    ElMessage.error('加载考试数据失败')
  } finally { loading.value = false }
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (heartbeatTimer) clearInterval(heartbeatTimer)
})

function onVisibilityChange() {
  if (document.hidden) { tabSwitchCount++ }
}

async function sendHeartbeat() {
  if (!sessionData.value) return
  try {
    await heartbeat(sessionData.value.id, tabSwitchCount)
  } catch {}
}

function getAnswer(questionId: number): string | undefined {
  return answers.value[questionId]
}

async function saveAnswer(q: any, val: string) {
  answers.value[q.questionId] = val
  try {
    await saveAnswer(sessionData.value.id, {
      questionId: q.questionId,
      sectionId: currentSection.value?.id,
      answerJson: val,
    })
  } catch {}
}

function navigateTo(questionId: number) { currentQuestionId.value = questionId }
function nextQuestion() {
  if (!isLast.value) currentQuestionId.value = allQuestions.value[currentIdx.value + 1].questionId
}
function prevQuestion() {
  if (!isFirst.value) currentQuestionId.value = allQuestions.value[currentIdx.value - 1].questionId
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确定交卷吗？交卷后不可再修改答案。', '提示', { type: 'warning' })
    submitting.value = true
    await submitExam(sessionData.value.id)
    ElMessage.success('交卷成功')
    router.push('/my-exams')
  } catch {
    // cancelled or error
  } finally { submitting.value = false }
}
</script>

<style scoped>
.exam-session { height: calc(100vh - 100px); display: flex; flex-direction: column; }
.exam-header { display: flex; justify-content: space-between; align-items: center;
  padding: 12px 20px; background: #fff; border-bottom: 1px solid #e6e6e6; }
.header-right { display: flex; align-items: center; gap: 20px; }
.exam-body { display: flex; flex: 1; overflow: hidden; background: #fff; }
.question-area { flex: 1; padding: 20px; overflow-y: auto; }
.question-meta { color: #909399; font-size: 13px; margin-bottom: 12px; }
.question-nav-buttons { text-align: center; margin-top: 24px; padding-top: 16px; border-top: 1px solid #eee; }
</style>
```

- [ ] **Step 6: 提交**

```bash
git add frontend/study-front/src/views/exam/MyExamsView.vue frontend/study-front/src/views/exam/ExamSessionView.vue frontend/study-front/src/views/exam/components/
git commit -m "feat(exam): add student exam taking pages"
```

---

### Task 14: 前端页面 — 成绩查看 + 批改

**文件：**
- Create: `frontend/study-front/src/views/exam/ExamResultView.vue`
- Create: `frontend/study-front/src/views/exam/GradingExamListView.vue`
- Create: `frontend/study-front/src/views/exam/GradingSessionView.vue`

- [ ] **Step 1: 创建 ExamResultView.vue**

```vue
<template>
  <div v-loading="loading">
    <el-row justify="space-between" align="middle">
      <h3>考试成绩</h3>
      <el-button @click="router.push('/my-exams')">返回</el-button>
    </el-row>

    <el-card style="margin-top: 16px" v-if="result">
      <el-statistic title="总分" :value="result.totalScore || 0" />
      <el-divider />

      <div v-for="ans in result.answers" :key="ans.questionId" class="answer-item">
        <div class="answer-header">
          <span class="question-text">{{ ans.questionContent }}</span>
          <el-tag :type="ans.isCorrect === 1 ? 'success' : 'danger'" size="small">
            {{ ans.gradingStatus === 'AUTO_GRADED' ? (ans.isCorrect === 1 ? '正确' : '错误') : '人工批改' }}
          </el-tag>
        </div>
        <div class="answer-detail">
          <div><b>你的答案：</b>{{ ans.studentAnswer || '未作答' }}</div>
          <div v-if="ans.gradingStatus === 'AUTO_GRADED'"><b>正确答案：</b>{{ ans.correctAnswer }}</div>
          <div v-if="ans.score !== null"><b>得分：</b>{{ ans.score }} / {{ ans.totalScore }}</div>
          <div v-if="ans.graderComment"><b>评语：</b>{{ ans.graderComment }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getExamResult } from '@/api/exam'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const result = ref<any>(null)

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await getExamResult(Number(route.params.sessionId))
    result.value = res.data
  } catch { ElMessage.error('加载失败')
  } finally { loading.value = false }
})
</script>

<style scoped>
.answer-item { padding: 12px; border: 1px solid #e6e6e6; border-radius: 4px; margin-bottom: 12px; }
.answer-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.question-text { font-weight: bold; flex: 1; }
.answer-detail { font-size: 14px; line-height: 1.8; color: #606266; }
</style>
```

- [ ] **Step 2: 创建 GradingExamListView.vue**

```vue
<template>
  <div>
    <h3>批改管理</h3>
    <el-table :data="exams" border stripe style="margin-top: 16px" v-loading="loading">
      <el-table-column prop="title" label="考试名称" min-width="200" />
      <el-table-column label="批改进度" width="150">
        <template #default="{ row }">{{ row.gradedSessions }}/{{ row.totalSessions }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary"
            @click="router.push(`/grading/exams/${row.id}/sessions`)">批改</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getGradingExams } from '@/api/exam'

const router = useRouter()
const loading = ref(false)
const exams = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  try { const res: any = await getGradingExams(); exams.value = res.data || [] }
  finally { loading.value = false }
})
</script>
```

- [ ] **Step 3: 创建 GradingSessionView.vue**

```vue
<template>
  <div v-loading="loading">
    <el-row justify="space-between" align="middle">
      <h3 v-if="!currentSession">选择学生答卷</h3>
      <h3 v-else>批改 - 学生答卷</h3>
      <el-button @click="currentSession ? (currentSession=null,fetchSessions()) : router.back()">
        {{ currentSession ? '返回列表' : '返回' }}
      </el-button>
    </el-row>

    <!-- 学生列表 -->
    <el-table v-if="!currentSession" :data="sessions" border stripe style="margin-top: 16px">
      <el-table-column prop="userId" label="学生ID" width="100" />
      <el-table-column prop="submittedAt" label="提交时间" width="170" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">{{ {SUBMITTED:'待批改',GRADING:'批改中',GRADED:'已批改'}[row.status] }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="loadSession(row.id)">批改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 批改界面 -->
    <div v-else style="margin-top: 16px">
      <el-card v-for="ans in currentSession.answers" :key="ans.questionId" style="margin-bottom: 12px">
        <div class="answer-header">
          <span><b>{{ ans.questionContent }}</b></span>
          <el-tag :type="ans.gradingStatus==='AUTO_GRADED'?'success':'warning'" size="small">
            {{ ans.gradingStatus==='AUTO_GRADED'?'自动批改':'待批改' }}
          </el-tag>
        </div>
        <div class="answer-detail">
          <div><b>学生答案：</b>{{ ans.studentAnswer || '未作答' }}</div>
          <div v-if="ans.correctAnswer"><b>参考答案：</b>{{ ans.correctAnswer }}</div>
          <div v-if="ans.gradingStatus === 'AUTO_GRADED'">
            <b>得分：</b>{{ ans.score }} / {{ ans.totalScore }}
          </div>
          <div v-else>
            <el-input-number v-model="gradeScores[ans.questionId]" :min="0" :max="ans.totalScore || 100" size="small" />
            <el-input v-model="gradeComments[ans.questionId]" placeholder="评语（可选）" size="small" style="width: 300px; margin-left: 8px" />
          </div>
        </div>
      </el-card>

      <div style="text-align: center; margin-top: 16px">
        <el-button type="primary" @click="submitGrading" :loading="saving">保存批改</el-button>
        <el-button type="success" @click="releaseGrade" :loading="releasing">发布成绩</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getGradingSessions, getSessionForGrading, gradeSession, releaseGrades } from '@/api/exam'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const releasing = ref(false)
const sessions = ref<any[]>([])
const currentSession = ref<any>(null)
const gradeScores = ref<Record<number, number>>({})
const gradeComments = ref<Record<number, string>>({})

onMounted(() => fetchSessions())

async function fetchSessions() {
  loading.value = true
  try { const res: any = await getGradingSessions(Number(route.params.examId)); sessions.value = res.data || [] }
  finally { loading.value = false }
}

async function loadSession(sessionId: number) {
  loading.value = true
  try {
    const res: any = await getSessionForGrading(sessionId)
    currentSession.value = res.data
    gradeScores.value = {}
    gradeComments.value = {}
  } finally { loading.value = false }
}

async function submitGrading() {
  saving.value = true
  try {
    const grades = Object.entries(gradeScores.value).filter(([, s]) => s !== undefined).map(([qId, score]) => ({
      questionId: Number(qId), score, comment: gradeComments.value[Number(qId)] || '',
    }))
    if (grades.length === 0) { ElMessage.warning('没有需要批改的题目'); return }
    await gradeSession(currentSession.value.id, { grades })
    ElMessage.success('批改保存成功')
  } catch {} finally { saving.value = false }
}

async function releaseGrade() {
  releasing.value = true
  try {
    await releaseGrades(currentSession.value.id)
    ElMessage.success('成绩发布成功')
    currentSession.value = null
    fetchSessions()
  } catch {} finally { releasing.value = false }
}
</script>

<style scoped>
.answer-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.answer-detail { font-size: 14px; line-height: 1.8; color: #606266; }
</style>
```

- [ ] **Step 4: 提交**

```bash
git add frontend/study-front/src/views/exam/ExamResultView.vue frontend/study-front/src/views/exam/GradingExamListView.vue frontend/study-front/src/views/exam/GradingSessionView.vue
git commit -m "feat(exam): add grading and result pages"
```

---

### Task 15: 前端编译验证

- [ ] **Step 1: 安装依赖 + 类型检查**

```bash
cd "D:/workspace/study-system/frontend/study-front"
npx vue-tsc --noEmit 2>&1
```

预期：无错误输出。

- [ ] **Step 2: 启动前端验证**

```bash
cd "D:/workspace/study-system/frontend/study-front"
npx vite --host 0.0.0.0 --port 5173 &
```

打开浏览器访问 http://localhost:5173/ ，确认页面可访问。

---

### Task 16: 端到端验证

- [ ] **Step 1: 确保所有服务运行**

```bash
docker ps  # MySQL/Redis/RabbitMQ
# 检查 java 进程是否包含 gateway, auth-service, question-service, paper-service
```

- [ ] **Step 2: 创建考试 + 分配**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# 创建考试
curl -s -X POST http://localhost:8080/api/exams \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  --data-binary '{"paperId":1,"title":"E2E Test Exam","timeMode":"FLEXIBLE","durationMinutes":30,"examCode":"TEST123"}'
```

- [ ] **Step 3: 学生开始考试 + 答题 + 提交**

```bash
# 学生登录（需要先有一个 student 用户或直接用 admin 测试）
S_TOKEN="$TOKEN"

# 查看我的考试
curl -s http://localhost:8080/api/my-exams -H "Authorization: Bearer $S_TOKEN"

# 开始考试
curl -s -X POST http://localhost:8080/api/my-exams/1/start \
  -H "Authorization: Bearer $S_TOKEN" | python -m json.tool | head -30
```

- [ ] **Step 4: 验证 API 全部通过后，提交最终版本**

```bash
git add -A
git commit -m "feat(exam): complete online exam module implementation"
```
