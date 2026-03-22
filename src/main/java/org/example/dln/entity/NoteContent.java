package org.example.dln.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.entity
 * 类名：NoteContent
 * 类描述：笔记正文实体类。
 * 创建人：@author Rain_润
 */
@Data
@TableName("t_note_content")
public class NoteContent {
    @TableId("note_id")
    private Long noteId;

    @TableField("markdown_content")
    private String markdownContent;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
