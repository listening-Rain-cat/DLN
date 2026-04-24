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
 * 类名：Note
 * 类描述：定义笔记基础信息实体。
 * 创建人：@author Rain_润
 */
@Data
@TableName(value = "t_note")
public class Note {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField(value = "folder_id")
    private Long folderId;

    @TableField(value = "title")
    private String title;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "delete_token")
    private Long deleteToken;

    @TableField(value = "deleted_time")
    private LocalDateTime deletedTime;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
