package com.study.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.auth.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select("SELECT role_id FROM user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);

    @Select("SELECT user_id FROM user_role WHERE role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(Long roleId);
}
