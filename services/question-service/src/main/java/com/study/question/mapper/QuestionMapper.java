package com.study.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.question.model.entity.Question;
import com.study.question.model.entity.KnowledgePoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("SELECT kp.* FROM knowledge_point kp " +
            "JOIN question_kp qkp ON kp.id = qkp.knowledge_point_id " +
            "WHERE qkp.question_id = #{questionId}")
    List<KnowledgePoint> selectKnowledgePointsByQuestionId(@Param("questionId") Long questionId);
}
