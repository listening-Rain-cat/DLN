package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建文件夹参数。
 */
@Data
public class CreateFolderDTO {
    @NotNull(message = "知识库 ID 不能为空")
    private Long knowledgeBaseId;

    private Long parentId;

    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 100, message = "文件夹名称不能超过 100 个字符")
    private String name;
}
