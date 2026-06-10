package com.study.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.auth.model.entity.StudentEnrollment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentEnrollmentMapper extends BaseMapper<StudentEnrollment> {
}
