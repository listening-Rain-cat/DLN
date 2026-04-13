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
 * 类名：NoteTag
 * 类描述：定义笔记与标签关联实体。
 * 创建人：@author Rain_润
 */
@Data
@TableName("t_note_tag")
public class NoteTag {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("note_id")
    private Long noteId;

    @TableField("tag_id")
    private Long tagId;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
