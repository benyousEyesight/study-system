package com.study.question.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question_kp")
public class QuestionKp {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long knowledgePointId;
}
