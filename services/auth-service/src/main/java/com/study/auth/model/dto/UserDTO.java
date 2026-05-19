package com.study.auth.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String userType;
    private String avatar;
    private Integer status;
    private List<String> roles;
    private List<String> permissions;
}
