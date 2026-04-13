package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.vo
 * 类名：NoteAttachmentVO
 * 类描述：定义笔记附件展示视图对象。
 * 创建人：@author Rain_润
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
