package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteHistoryDetailVO {
    private String id;
    private String noteId;
    private Integer versionNo;
    private String title;
    private String markdownContent;
    private String createdBy;
    private LocalDateTime createdTime;
}
