package com.study.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.question.model.entity.Subject;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubjectMapper extends BaseMapper<Subject> {
}
