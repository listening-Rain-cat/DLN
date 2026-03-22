package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改知识库参数。
 */
@Data
public class UpdateKnowledgeBaseDTO {
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称不能超过 100 个字符")
    private String name;

    @Size(max = 255, message = "知识库描述不能超过 255 个字符")
    private String description;
}
