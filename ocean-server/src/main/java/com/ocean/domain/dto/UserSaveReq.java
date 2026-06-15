package com.ocean.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UserSaveReq {

    private Long id;

    @NotBlank(message = "登录名不能为空", groups = {Create.class})
    @Size(max = 50, message = "登录名最大50字符")
    private String loginName;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最大50字符")
    private String name;

    @NotBlank(message = "密码不能为空", groups = {Create.class})
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,32}$", message = "密码必须6-32位，包含数字和字母", groups = {Create.class})
    private String password;

    private String role;

    public interface Create {}
    public interface Update {}
}
