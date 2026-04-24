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
 * 类名：NoteLink
 * 类描述：定义笔记双链关系实体。
 * 创建人：@author Rain_润
 */
@Data
@TableName(value = "t_note_link")
public class NoteLink {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField(value = "source_note_id")
    private Long sourceNoteId;

    @TableField(value = "target_note_id")
    private Long targetNoteId;

    @TableField(value = "target_note_name")
    private String targetNoteName;

    @TableField(value = "anchor_text")
    private String anchorText;

    @TableField(value = "context_snippet")
    private String contextSnippet;

    @TableField(value = "is_broken")
    private Integer isBroken;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
