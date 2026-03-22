package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建标签参数。
 */
@Data
public class CreateTagDTO {
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 100, message = "标签名称不能超过 100 个字符")
    private String name;
}
