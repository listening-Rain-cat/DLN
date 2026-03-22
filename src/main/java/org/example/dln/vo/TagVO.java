package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签返回对象。
 */
@Data
public class TagVO {
    private Long id;
    private Long knowledgeBaseId;
    private String name;
    private LocalDateTime createdTime;
}
