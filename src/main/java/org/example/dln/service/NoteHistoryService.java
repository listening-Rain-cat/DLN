package org.example.dln.service;

import org.example.dln.dto.UpdateNoteDTO;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteContent;
import org.example.dln.entity.NoteHistory;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteContentMapper;
import org.example.dln.mapper.NoteHistoryMapper;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.util.LongStringUtils;
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

    /**
    * 查询笔记历史版本列表。
     * @param userId 用户ID
     * @param noteId 笔记ID
    */
    public List<NoteHistoryVO> listNoteHistories(Long userId, Long noteId) {
        getNoteOrThrow(userId, noteId);
        return noteHistoryMapper.selectByNoteIdOrderByVersionNoDesc(noteId)
                .stream()
                .map(this::toNoteHistoryVO)
                .toList();
    }

    /**
    * 获取历史版本详情。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param historyId 历史版本ID
    */
    public NoteHistoryDetailVO getNoteHistoryDetail(Long userId, Long noteId, Long historyId) {
        getNoteOrThrow(userId, noteId);
        return toNoteHistoryDetailVO(getHistoryOrThrow(noteId, historyId));
    }

    /**
    * 创建历史版本快照。
     * @param userId 用户ID
     * @param noteId 笔记ID
    */
    @Transactional(rollbackFor = Exception.class)
    public NoteHistoryVO createHistorySnapshot(Long userId, Long noteId) {
        Note note = getNoteOrThrow(userId, noteId);
        NoteContent noteContent = noteContentMapper.selectByNoteId(noteId);
        String markdownContent = noteContent == null ? null : noteContent.getMarkdownContent();
        return toNoteHistoryVO(saveHistoryIfNeeded(note, markdownContent, userId));
    }

    /**
    * 恢复历史版本到当前笔记。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param historyId 历史版本ID
    */
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

    /**
    * 删除历史版本。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param historyId 历史版本ID
    */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNoteHistory(Long userId, Long noteId, Long historyId) {
        getNoteOrThrow(userId, noteId);
        getHistoryOrThrow(noteId, historyId);

        if (noteHistoryMapper.deleteByNoteIdAndHistoryId(noteId, historyId) <= 0) {
            throw new BusinessException("删除历史版本失败");
        }
    }

    /**
    * 获取笔记，不存在时抛出异常。
     * @param userId 用户ID
     * @param noteId 笔记ID
    */
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

    /**
    * 获取历史版本，不存在时抛出异常。
     * @param noteId 笔记ID
     * @param historyId 历史版本ID
    */
    private NoteHistory getHistoryOrThrow(Long noteId, Long historyId) {
        NoteHistory noteHistory = noteHistoryMapper.selectByNoteIdAndHistoryId(noteId, historyId);
        if (noteHistory == null) {
            throw new BusinessException("历史版本不存在");
        }
        return noteHistory;
    }

    /**
    * 按需保存历史版本。
     * @param note 笔记实体
     * @param markdownContent Markdown内容
     * @param userId 用户ID
    */
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

    /**
    * 将历史版本实体转换为历史版本视图对象。
     * @param noteHistory 历史版本实体
    */
    private NoteHistoryVO toNoteHistoryVO(NoteHistory noteHistory) {
        NoteHistoryVO vo = new NoteHistoryVO();
        BeanUtils.copyProperties(noteHistory, vo);
        vo.setId(LongStringUtils.toStringValue(noteHistory.getId()));
        vo.setNoteId(LongStringUtils.toStringValue(noteHistory.getNoteId()));
        vo.setCreatedBy(LongStringUtils.toStringValue(noteHistory.getCreatedBy()));
        return vo;
    }

    /**
    * 将历史版本实体转换为历史版本详情视图对象。
     * @param noteHistory 历史版本实体
    */
    private NoteHistoryDetailVO toNoteHistoryDetailVO(NoteHistory noteHistory) {
        NoteHistoryDetailVO vo = new NoteHistoryDetailVO();
        BeanUtils.copyProperties(noteHistory, vo);
        vo.setId(LongStringUtils.toStringValue(noteHistory.getId()));
        vo.setNoteId(LongStringUtils.toStringValue(noteHistory.getNoteId()));
        vo.setCreatedBy(LongStringUtils.toStringValue(noteHistory.getCreatedBy()));
        return vo;
    }
}
