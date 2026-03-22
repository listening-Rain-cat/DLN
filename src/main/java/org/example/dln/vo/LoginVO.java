package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

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
