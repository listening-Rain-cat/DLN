package org.example.dln.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 包名：org.example.dln.vo
 * 类名：KnowledgeGraphVO
 * 类描述：定义知识图谱展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class KnowledgeGraphVO {
    private Long knowledgeBaseId;
    private List<KnowledgeGraphNodeVO> nodes = new ArrayList<>();
    private List<KnowledgeGraphEdgeVO> edges = new ArrayList<>();
}
