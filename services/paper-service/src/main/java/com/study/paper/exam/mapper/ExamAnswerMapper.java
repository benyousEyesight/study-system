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
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectQuestionsByIds(@Param("ids") List<Long> ids);
}
