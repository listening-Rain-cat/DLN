package org.example.dln.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class NoteSearchResultVO {
    private String noteId;
    private String knowledgeBaseId;
    private String folderId;
    private String title;
    private String folderPath;
    private String snippet;
    private Boolean matchedByTitle;
    private Boolean matchedByContent;
    private Boolean matchedByTag;
    private Integer incomingCount;
    private Integer outgoingCount;
    private Integer brokenLinkCount;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private List<TagVO> tags = new ArrayList<>();
}
