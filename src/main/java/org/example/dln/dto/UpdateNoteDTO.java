package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改笔记参数。
 */
@Data
public class UpdateNoteDTO {
    private Long folderId;

    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 255, message = "笔记标题不能超过 255 个字符")
    private String title;

    private String markdownContent;
}
