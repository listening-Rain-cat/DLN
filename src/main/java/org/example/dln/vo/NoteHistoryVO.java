package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteHistoryVO {
    private String id;
    private String noteId;
    private Integer versionNo;
    private String title;
    private String createdBy;
    private LocalDateTime createdTime;
}
