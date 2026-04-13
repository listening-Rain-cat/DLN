package org.example.dln.dto;

import lombok.Data;

import java.util.List;

/**
 * 包名：org.example.dln.dto
 * 类名：SetNoteTagsDTO
 * 类描述：用于承载设置笔记标签请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class SetNoteTagsDTO {
    private List<Long> tagIds;
}
