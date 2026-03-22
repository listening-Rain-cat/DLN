package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库返回对象。
 */
@Data
public class KnowledgeBaseVO {
    private Long id;
    private String name;
    private String description;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
