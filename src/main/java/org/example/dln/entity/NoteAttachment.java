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
 * 类名：NoteAttachment
 * 类描述：定义笔记附件实体。
 * 创建人：@author Rain_润
 */
@Data
@TableName(value = "t_note_attachment")
public class NoteAttachment {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "note_id")
    private Long noteId;

    @TableField(value = "file_name")
    private String fileName;

    @TableField(value = "file_type")
    private String fileType;

    @TableField(value = "file_url")
    private String fileUrl;

    @TableField(value = "file_size")
    private Long fileSize;

    @TableField(value = "mime_type")
    private String mimeType;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
