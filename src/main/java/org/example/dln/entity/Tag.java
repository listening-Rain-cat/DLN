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
 * 类名：Tag
 * 类描述：标签实体类，映射到数据库表 t_tag。
 * 创建人：@author Rain_润
 */
@Data
@TableName("t_tag")
public class Tag {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("name")
    private String name;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
