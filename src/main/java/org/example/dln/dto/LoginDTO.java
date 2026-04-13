package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：LoginDTO
 * 类描述：用于承载用户登录请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
