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
 * 类描述：笔记附件实体类。
 * 创建人：@author Rain_润
 */
@Data
@TableName("t_note_attachment")
public class NoteAttachment {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("note_id")
    private Long noteId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("file_url")
    private String fileUrl;

    @TableField("file_size")
    private Long fileSize;

    @TableField("mime_type")
    private String mimeType;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
