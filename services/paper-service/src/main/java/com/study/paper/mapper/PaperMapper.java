package com.study.paper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.paper.model.entity.Paper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
}
