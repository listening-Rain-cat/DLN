package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：CreateTagDTO
 * 类描述：用于承载创建标签请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class CreateTagDTO {
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 100, message = "标签名称不能超过 100 个字符")
    private String name;
}
