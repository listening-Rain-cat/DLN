package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteHistoryVO {
    private Long id;
    private Long noteId;
    private Integer versionNo;
    private String title;
    private Long createdBy;
    private LocalDateTime createdTime;
}
