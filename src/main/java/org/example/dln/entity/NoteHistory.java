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
 * 类描述：笔记历史版本实体类。
 * 创建人：@author Rain_润
 */
@Data
@TableName("t_note_history")
public class NoteHistory {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("note_id")
    private Long noteId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("title")
    private String title;

    @TableField("markdown_content")
    private String markdownContent;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
