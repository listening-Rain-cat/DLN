package org.example.dln.service;

import org.example.dln.config.CacheNames;
import org.example.dln.entity.Note;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.util.LongStringUtils;
import org.example.dln.vo.NoteLinkCandidateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 包名：org.example.dln.service
 * 类名：NoteLinkCandidateCacheService
 * 类描述：缓存知识库内可作为双链目标的笔记候选项。
 * 创建人：@author Rain_润
 */
@Service
public class NoteLinkCandidateCacheService {
    @Autowired
    private NoteMapper noteMapper;

    /**
    * 查询并缓存知识库内所有有效笔记候选项。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
    */
    @Cacheable(cacheNames = CacheNames.NOTE_LINK_CANDIDATES, key = "#userId + ':' + #knowledgeBaseId", sync = true)
    public List<NoteLinkCandidateVO> listKnowledgeBaseCandidates(Long userId, Long knowledgeBaseId) {
        List<Note> notes = noteMapper.selectActiveByUserIdAndKnowledgeBaseIdOrderByTitleAsc(userId, knowledgeBaseId);
        List<NoteLinkCandidateVO> result = new ArrayList<>();
        for (Note note : notes) {
            String title = note.getTitle();
            if (title == null || title.isBlank()) {
                continue;
            }

            NoteLinkCandidateVO candidate = new NoteLinkCandidateVO();
            candidate.setNoteId(LongStringUtils.toStringValue(note.getId()));
            candidate.setFolderId(LongStringUtils.toStringValue(note.getFolderId()));
            candidate.setTitle(title);
            result.add(candidate);
        }
        return result;
    }
}
