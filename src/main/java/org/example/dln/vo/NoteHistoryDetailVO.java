package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteHistoryDetailVO {
    private Long id;
    private Long noteId;
    private Integer versionNo;
    private String title;
    private String markdownContent;
    private Long createdBy;
    private LocalDateTime createdTime;
}
