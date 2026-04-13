package org.example.dln.dto;

import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：AutoSaveNoteContentDTO
 * 类描述：用于承载笔记内容自动保存请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class AutoSaveNoteContentDTO {
    private String markdownContent;
}
