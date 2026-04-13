package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：UpdateUserSettingsDTO
 * 类描述：用于承载更新用户设置请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class UpdateUserSettingsDTO {
    @NotBlank(message = "代码主题不能为空")
    @Size(max = 255, message = "代码主题长度不能超过 255")
    private String codeTheme;

    @NotBlank(message = "内容主题不能为空")
    @Size(max = 255, message = "内容主题长度不能超过 255")
    private String contentTheme;
}
