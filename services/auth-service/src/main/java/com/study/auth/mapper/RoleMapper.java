package com.study.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.auth.model.entity.Role;
import com.study.auth.model.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    @Select("SELECT p.* FROM permission p " +
            "JOIN role_permission rp ON p.id = rp.permission_id " +
            "JOIN user_role ur ON ur.role_id = rp.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Permission> selectPermissionsByUserId(@Param("userId") Long userId);
}
