package com.ocean.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UserProfileUpdateReq {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最大50字符")
    private String name;
}
