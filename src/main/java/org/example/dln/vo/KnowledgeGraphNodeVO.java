package org.example.dln.vo;

import lombok.Data;

/**
 * 包名：org.example.dln.vo
 * 类名：KnowledgeGraphNodeVO
 * 类描述：定义知识图谱节点展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class KnowledgeGraphNodeVO {
    private Long noteId;
    private Long folderId;
    private String title;
    private Integer incomingCount;
    private Integer outgoingCount;
}
