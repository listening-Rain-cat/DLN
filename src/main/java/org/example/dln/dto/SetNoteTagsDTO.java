package org.example.dln.dto;

import lombok.Data;

import java.util.List;

/**
 * 设置笔记标签参数。
 */
@Data
public class SetNoteTagsDTO {
    private List<Long> tagIds;
}
