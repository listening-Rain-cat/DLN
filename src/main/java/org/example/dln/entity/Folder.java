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
 * 类名：Folder
 * 类描述：定义知识库文件夹实体。
 * 创建人：@author Rain_润
 */
@Data
@TableName("t_folder")
public class Folder {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("status")
    private Integer status;

    @TableField("delete_token")
    private Long deleteToken;

    @TableField("deleted_time")
    private LocalDateTime deletedTime;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
