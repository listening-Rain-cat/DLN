package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件树节点返回对象。
 */
@Data
public class TreeNodeVO {
    private Long id;
    private Long parentId;
    private String name;
    private String type;
    private Long knowledgeBaseId;
    private LocalDateTime updatedTime;
    private List<TreeNodeVO> children = new ArrayList<>();
}
