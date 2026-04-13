package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：UpdateNoteTemplateDTO
 * 类描述：用于承载更新笔记模板请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class UpdateNoteTemplateDTO {
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过 100 个字符")
    private String name;

    @Size(max = 255, message = "模板描述不能超过 255 个字符")
    private String description;

    @NotBlank(message = "模板内容不能为空")
    private String templateContent;
}
