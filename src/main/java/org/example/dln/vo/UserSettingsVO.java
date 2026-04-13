package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.vo
 * 类名：UserSettingsVO
 * 类描述：定义用户设置展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class UserSettingsVO {
    private Long id;
    private Long userId;
    private String codeTheme;
    private String contentTheme;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
