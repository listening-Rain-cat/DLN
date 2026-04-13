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
 * 包名：org.example.dln.entity
 * 类名：User
 * 类描述：定义用户实体。
 * 创建人：@author Rain_润
 */
@Data
@ToString
@TableName("t_user")
public class User {
    // 数据库没有自增 ID，所以使用 ASSIGN_ID 策略，MyBatis-Plus 会自动生成唯一 ID
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
     * 状态：1 正常，0 禁用
     */
    @TableField("status")
    private Integer status;
    /**
     * 创建时间，与更新时间自动填充
     */

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}