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
    private String id;
    private String noteId;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private String fileSize;
    private String mimeType;
    private LocalDateTime createdTime;
}
