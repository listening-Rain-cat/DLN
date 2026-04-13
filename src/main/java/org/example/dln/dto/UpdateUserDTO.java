package org.example.dln.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：UpdateUserDTO
 * 类描述：用于承载更新用户信息请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class UpdateUserDTO {
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 个字符之间")
    private String oldPassword;

    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 个字符之间")
    private String newPassword;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称不能超过 20 个字符")
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 255, message = "头像地址不能超过 255 个字符")
    private String avatarUrl;
}
