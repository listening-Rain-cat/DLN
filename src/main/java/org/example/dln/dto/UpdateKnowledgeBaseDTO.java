package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：UpdateKnowledgeBaseDTO
 * 类描述：用于承载更新知识库请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class UpdateKnowledgeBaseDTO {
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称不能超过 100 个字符")
    private String name;

    @Size(max = 255, message = "知识库描述不能超过 255 个字符")
    private String description;
}
