package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记详情返回对象。
 */
@Data
public class NoteDetailVO {
    private Long id;
    private Long knowledgeBaseId;
    private Long folderId;
    private String title;
    private Integer status;
    private String markdownContent;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private List<TagVO> tags;
    private List<NoteAttachmentVO> attachments;
    private List<NoteLinkVO> outgoingLinks;
    private List<NoteLinkVO> incomingLinks;
}
