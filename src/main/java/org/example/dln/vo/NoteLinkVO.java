package org.example.dln.vo;

import lombok.Data;

/**
 * 双链返回对象。
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
