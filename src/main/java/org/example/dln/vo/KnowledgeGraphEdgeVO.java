package org.example.dln.vo;

import lombok.Data;

/**
 * 包名：org.example.dln.vo
 * 类名：KnowledgeGraphEdgeVO
 * 类描述：定义知识图谱边展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class KnowledgeGraphEdgeVO {
    private String id;
    private String sourceNoteId;
    private String targetNoteId;
    private String targetNoteName;
    private Integer isBroken;
}
