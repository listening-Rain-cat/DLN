package org.example.dln.service;

import org.example.dln.dto.UpdateNoteDTO;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteContent;
import org.example.dln.entity.NoteHistory;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteContentMapper;
import org.example.dln.mapper.NoteHistoryMapper;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.vo.NoteDetailVO;
import org.example.dln.vo.NoteHistoryDetailVO;
import org.example.dln.vo.NoteHistoryVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class NoteHistoryService {
    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteContentMapper noteContentMapper;

    @Autowired
    private NoteHistoryMapper noteHistoryMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private NoteService noteService;

    public List<NoteHistoryVO> listNoteHistories(Long userId, Long noteId) {
        getNoteOrThrow(userId, noteId);
        return noteHistoryMapper.selectByNoteIdOrderByVersionNoDesc(noteId)
                .stream()
                .map(this::toNoteHistoryVO)
                .toList();
    }

    public NoteHistoryDetailVO getNoteHistoryDetail(Long userId, Long noteId, Long historyId) {
        getNoteOrThrow(userId, noteId);
        return toNoteHistoryDetailVO(getHistoryOrThrow(noteId, historyId));
    }

    @Transactional(rollbackFor = Exception.class)
    public NoteHistoryVO createHistorySnapshot(Long userId, Long noteId) {
        Note note = getNoteOrThrow(userId, noteId);
        NoteContent noteContent = noteContentMapper.selectByNoteId(noteId);
        String markdownContent = noteContent == null ? null : noteContent.getMarkdownContent();
        return toNoteHistoryVO(saveHistoryIfNeeded(note, markdownContent, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO restoreNoteHistory(Long userId, Long noteId, Long historyId) {
        Note note = getNoteOrThrow(userId, noteId);
        NoteHistory noteHistory = getHistoryOrThrow(noteId, historyId);

        UpdateNoteDTO dto = new UpdateNoteDTO();
        dto.setFolderId(note.getFolderId());
        dto.setTitle(noteHistory.getTitle());
        dto.setMarkdownContent(noteHistory.getMarkdownContent());
        return noteService.updateNote(userId, noteId, dto);
    }

    private Note getNoteOrThrow(Long userId, Long noteId) {
        Note note = noteMapper.selectByNoteId(noteId);
        if (note == null
                || !Objects.equals(note.getUserId(), userId)
                || note.getStatus() == null
                || note.getStatus() != 1
                || !Objects.equals(note.getDeleteToken(), 0L)) {
            throw new BusinessException("笔记不存在或无权限访问");
        }
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, note.getKnowledgeBaseId());
        return note;
    }

    private NoteHistory getHistoryOrThrow(Long noteId, Long historyId) {
        NoteHistory noteHistory = noteHistoryMapper.selectByNoteIdAndHistoryId(noteId, historyId);
        if (noteHistory == null) {
            throw new BusinessException("历史版本不存在");
        }
        return noteHistory;
    }

    private NoteHistory saveHistoryIfNeeded(Note note, String markdownContent, Long userId) {
        NoteHistory latest = noteHistoryMapper.selectLatestByNoteId(note.getId());
        if (latest != null
                && Objects.equals(latest.getTitle(), note.getTitle())
                && Objects.equals(latest.getMarkdownContent(), markdownContent)) {
            return latest;
        }

        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        NoteHistory noteHistory = new NoteHistory();
        noteHistory.setNoteId(note.getId());
        noteHistory.setVersionNo(versionNo);
        noteHistory.setTitle(note.getTitle());
        noteHistory.setMarkdownContent(markdownContent);
        noteHistory.setCreatedBy(userId);
        if (noteHistoryMapper.insert(noteHistory) <= 0) {
            throw new BusinessException("创建历史版本失败");
        }
        return noteHistory;
    }

    private NoteHistoryVO toNoteHistoryVO(NoteHistory noteHistory) {
        NoteHistoryVO vo = new NoteHistoryVO();
        BeanUtils.copyProperties(noteHistory, vo);
        return vo;
    }

    private NoteHistoryDetailVO toNoteHistoryDetailVO(NoteHistory noteHistory) {
        NoteHistoryDetailVO vo = new NoteHistoryDetailVO();
        BeanUtils.copyProperties(noteHistory, vo);
        return vo;
    }
}
