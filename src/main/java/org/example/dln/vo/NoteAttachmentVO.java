package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件返回对象。
 */
@Data
public class NoteAttachmentVO {
    private Long id;
    private Long noteId;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdTime;
}
