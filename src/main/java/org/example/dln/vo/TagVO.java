package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.vo
 * 类名：TagVO
 * 类描述：定义标签展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class TagVO {
    private Long id;
    private Long knowledgeBaseId;
    private String name;
    private LocalDateTime createdTime;
}
