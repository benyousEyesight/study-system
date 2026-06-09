package com.study.paper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExternalQuestionRefMapper {

    @Select("SELECT id, type, content_json AS contentJson, difficulty, status " +
            "FROM question_db.question WHERE id = #{id}")
    Map<String, Object> selectQuestionById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT id, type, content_json AS contentJson, difficulty, status " +
            "FROM question_db.question WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectQuestionsByIds(@Param("ids") List<Long> ids);

    @Select("<script>" +
            "SELECT q.id, q.type, q.content_json AS contentJson, q.difficulty, q.status " +
            "FROM question_db.question q " +
            "WHERE q.subject_id = #{subjectId} " +
            "AND q.type = #{type} " +
            "AND q.status = 'PUBLISHED' " +
            "<if test='difficulty != null'> AND q.difficulty = #{difficulty} </if>" +
            "<if test='kpIds != null and kpIds.size() &gt; 0'>" +
            " AND q.id IN (SELECT qkp.question_id FROM question_db.question_kp qkp " +
            " WHERE qkp.knowledge_point_id IN " +
            "<foreach collection='kpIds' item='kpId' open='(' separator=',' close=')'>#{kpId}</foreach>" +
            " GROUP BY qkp.question_id HAVING COUNT(DISTINCT qkp.knowledge_point_id) = #{kpCount})" +
            "</if>" +
            "</script>")
    List<Map<String, Object>> selectQuestionsForGenerate(@Param("subjectId") Long subjectId,
                                                         @Param("type") String type,
                                                         @Param("difficulty") Object difficulty,
                                                         @Param("kpIds") List<Long> kpIds,
                                                         @Param("kpCount") Integer kpCount);
}
