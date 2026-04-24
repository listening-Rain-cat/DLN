package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.vo
 * 类名：NoteTemplateVO
 * 类描述：定义笔记模板展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class NoteTemplateVO {
    private String id;
    private String name;
    private String description;
    private String templateContent;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
