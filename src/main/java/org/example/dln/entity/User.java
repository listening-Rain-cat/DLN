package org.example.dln.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 包名称： org.example.dln.entity
 * 类名称：User
 * 类描述：用户实体类，映射到数据库中的t_user表
 * 创建人：@author Rain_润
 * 创建时间：2026-03-18 21:11
 * 建议每个属性都添加TableField注解，明确指定数据库字段名。
 */
@Data
@ToString
@TableName("t_user")
public class User {
    //数据库没有自增ID，所以使用ASSIGN_ID策略，MyBatis-Plus会自动生成一个唯一ID
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("username")
    private String username;
    @TableField("email")
    private String email;
    @TableField("password")
    private String password;
    @TableField("nickname")
    private String nickname;
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 状态：1正常 0禁用
     */
    @TableField("status")
    private Integer status;
    /**
     * 创建时间，与更改时间自动填充
     */

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}