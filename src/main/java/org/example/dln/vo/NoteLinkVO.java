package org.example.dln.vo;

import lombok.Data;

/**
 * 包名：org.example.dln.vo
 * 类名：NoteLinkVO
 * 类描述：定义笔记双链展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class NoteLinkVO {
    private Long id;
    private Long sourceNoteId;
    private Long targetNoteId;
    private String targetNoteName;
    private String anchorText;
    private String contextSnippet;
    private Integer isBroken;
}
