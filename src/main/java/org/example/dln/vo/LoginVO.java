package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.vo
 * 类名：LoginVO
 * 类描述：定义登录结果展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class LoginVO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String token;
}
