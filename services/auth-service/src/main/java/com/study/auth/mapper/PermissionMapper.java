package com.study.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.auth.model.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
