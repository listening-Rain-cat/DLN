package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.vo
 * 类名：KnowledgeBaseVO
 * 类描述：定义知识库展示视图对象。
 * 创建人：@author Rain_润
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
