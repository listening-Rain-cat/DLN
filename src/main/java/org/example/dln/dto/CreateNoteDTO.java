package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：CreateNoteDTO
 * 类描述：用于承载创建笔记请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class CreateNoteDTO {
    @NotNull(message = "知识库 ID 不能为空")
    private Long knowledgeBaseId;

    private Long folderId;

    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 255, message = "笔记标题不能超过 255 个字符")
    private String title;

    private Long templateId;

    private String markdownContent;
}
