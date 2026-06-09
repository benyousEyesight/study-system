package com.study.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.auth.model.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Select("SELECT permission_id FROM role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(Long roleId);
}
