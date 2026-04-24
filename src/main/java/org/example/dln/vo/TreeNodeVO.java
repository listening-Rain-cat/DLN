package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 包名：org.example.dln.vo
 * 类名：TreeNodeVO
 * 类描述：定义树形节点展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class TreeNodeVO {
    private String id;
    private String parentId;
    private String name;
    private String type;
    private String knowledgeBaseId;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private List<TagVO> tags = new ArrayList<>();
    private List<TreeNodeVO> children = new ArrayList<>();
}
