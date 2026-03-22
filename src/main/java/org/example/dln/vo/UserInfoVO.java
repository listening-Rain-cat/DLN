package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.vo
 * 类名：UserInfoVO
 * 类描述：用户信息返回对象，用于向前端返回不含密码的用户资料。
 * 创建人：@author Rain_润
 */
@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
