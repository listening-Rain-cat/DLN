package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 包名：org.example.dln.vo
 * 类名：NoteDetailVO
 * 类描述：定义笔记详情展示视图对象。
 * 创建人：@author Rain_润
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
