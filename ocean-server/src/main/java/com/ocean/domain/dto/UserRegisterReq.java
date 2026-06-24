package com.ocean.domain.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UserRegisterReq {

    @NotBlank(message = "登录名不能为空")
    @Size(max = 50, message = "登录名最大50字符")
    private String loginName;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最大50字符")
    private String name;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,32}$",
            message = "密码必须6-32位，包含数字和字母")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位数字")
    private String emailCode;
}
