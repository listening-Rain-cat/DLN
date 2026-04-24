package org.example.dln.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.entity
 * 类名：NoteHistory
 * 类描述：定义笔记历史记录实体。
 * 创建人：@author Rain_润
 */
@Data
@TableName(value = "t_note_history")
public class NoteHistory {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "note_id")
    private Long noteId;

    @TableField(value = "version_no")
    private Integer versionNo;

    @TableField(value = "title")
    private String title;

    @TableField(value = "markdown_content")
    private String markdownContent;

    @TableField(value = "created_by")
    private Long createdBy;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
