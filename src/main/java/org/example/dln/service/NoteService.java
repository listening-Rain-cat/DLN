package org.example.dln.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.dln.dto.CreateNoteDTO;
import org.example.dln.dto.UpdateNoteDTO;
import org.example.dln.entity.Folder;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteContent;
import org.example.dln.entity.NoteHistory;
import org.example.dln.entity.NoteLink;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteContentMapper;
import org.example.dln.mapper.NoteHistoryMapper;
import org.example.dln.mapper.NoteLinkMapper;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.vo.NoteDetailVO;
import org.example.dln.vo.NoteLinkVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 笔记业务逻辑，包含正文、历史和双链处理。
 */
@Service
public class NoteService {
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[([^\\[\\]]+)]]");

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteContentMapper noteContentMapper;

    @Autowired
    private NoteHistoryMapper noteHistoryMapper;

    @Autowired
    private NoteLinkMapper noteLinkMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private TagService tagService;

    @Autowired
    private AttachmentService attachmentService;

    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO createNote(Long userId, CreateNoteDTO dto) {
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, dto.getKnowledgeBaseId());
        validateFolder(dto.getKnowledgeBaseId(), dto.getFolderId());
        String title = dto.getTitle().trim();
        checkNoteTitleExists(dto.getKnowledgeBaseId(), title, null);

        Note note = new Note();
        note.setUserId(userId);
        note.setKnowledgeBaseId(dto.getKnowledgeBaseId());
        note.setFolderId(dto.getFolderId());
        note.setTitle(title);
        note.setStatus(1);
        if (noteMapper.insert(note) <= 0) {
            throw new BusinessException("创建笔记失败");
        }

        NoteContent noteContent = new NoteContent();
        noteContent.setNoteId(note.getId());
        noteContent.setMarkdownContent(dto.getMarkdownContent());
        if (noteContentMapper.insert(noteContent) <= 0) {
            throw new BusinessException("保存笔记内容失败");
        }

        saveHistory(note, dto.getMarkdownContent(), userId);
        refreshLinks(note, dto.getMarkdownContent());
        return getNoteDetail(userId, note.getId());
    }

    public NoteDetailVO getNoteDetail(Long userId, Long noteId) {
        Note note = getNoteOrThrow(userId, noteId);
        NoteContent noteContent = noteContentMapper.selectById(noteId);

        NoteDetailVO vo = new NoteDetailVO();
        BeanUtils.copyProperties(note, vo);
        vo.setMarkdownContent(noteContent == null ? null : noteContent.getMarkdownContent());
        vo.setTags(tagService.listNoteTags(userId, noteId));
        vo.setAttachments(attachmentService.listNoteAttachments(userId, noteId));
        vo.setOutgoingLinks(buildLinkVOList(noteLinkMapper.selectList(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getSourceNoteId, noteId)
                .orderByAsc(NoteLink::getTargetNoteName))));
        vo.setIncomingLinks(buildLinkVOList(noteLinkMapper.selectList(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getTargetNoteId, noteId)
                .orderByAsc(NoteLink::getSourceNoteId))));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO updateNote(Long userId, Long noteId, UpdateNoteDTO dto) {
        Note note = getNoteOrThrow(userId, noteId);
        validateFolder(note.getKnowledgeBaseId(), dto.getFolderId());
        String title = dto.getTitle().trim();
        checkNoteTitleExists(note.getKnowledgeBaseId(), title, noteId);

        note.setFolderId(dto.getFolderId());
        note.setTitle(title);
        if (noteMapper.updateById(note) <= 0) {
            throw new BusinessException("更新笔记失败");
        }

        NoteContent noteContent = noteContentMapper.selectById(noteId);
        if (noteContent == null) {
            noteContent = new NoteContent();
            noteContent.setNoteId(noteId);
            noteContent.setMarkdownContent(dto.getMarkdownContent());
            if (noteContentMapper.insert(noteContent) <= 0) {
                throw new BusinessException("保存笔记内容失败");
            }
        } else {
            noteContent.setMarkdownContent(dto.getMarkdownContent());
            if (noteContentMapper.updateById(noteContent) <= 0) {
                throw new BusinessException("更新笔记内容失败");
            }
        }

        saveHistory(note, dto.getMarkdownContent(), userId);
        refreshLinks(note, dto.getMarkdownContent());
        return getNoteDetail(userId, noteId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long userId, Long noteId) {
        Note note = getNoteOrThrow(userId, noteId);
        note.setStatus(2);
        note.setDeletedTime(LocalDateTime.now());
        if (noteMapper.updateById(note) <= 0) {
            throw new BusinessException("删除笔记失败");
        }
    }

    private Note getNoteOrThrow(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || !Objects.equals(note.getUserId(), userId) || note.getStatus() == null || note.getStatus() != 1) {
            throw new BusinessException("笔记不存在或无权限访问");
        }
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, note.getKnowledgeBaseId());
        return note;
    }

    private void validateFolder(Long knowledgeBaseId, Long folderId) {
        if (folderId == null) {
            return;
        }
        Folder folder = knowledgeBaseService.getFolderInKnowledgeBaseOrThrow(knowledgeBaseId, folderId);
        if (folder.getStatus() == null || folder.getStatus() != 1) {
            throw new BusinessException("文件夹不存在");
        }
    }

    private void checkNoteTitleExists(Long knowledgeBaseId, String title, Long excludeId) {
        List<Note> notes = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .eq(Note::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Note::getTitle, title)
                .eq(Note::getStatus, 1));
        boolean exists = notes.stream().anyMatch(note -> !Objects.equals(note.getId(), excludeId));
        if (exists) {
            throw new BusinessException("当前知识库下已存在同名笔记");
        }
    }

    private void saveHistory(Note note, String markdownContent, Long userId) {
        NoteHistory latest = noteHistoryMapper.selectOne(new LambdaQueryWrapper<NoteHistory>()
                .eq(NoteHistory::getNoteId, note.getId())
                .orderByDesc(NoteHistory::getVersionNo)
                .last("limit 1"));
        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;

        NoteHistory history = new NoteHistory();
        history.setNoteId(note.getId());
        history.setVersionNo(versionNo);
        history.setTitle(note.getTitle());
        history.setMarkdownContent(markdownContent);
        history.setCreatedBy(userId);
        noteHistoryMapper.insert(history);
    }

    private void refreshLinks(Note note, String markdownContent) {
        noteLinkMapper.delete(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getSourceNoteId, note.getId()));
        if (markdownContent == null || markdownContent.isBlank()) {
            refreshBrokenLinks(note.getKnowledgeBaseId(), note.getId());
            return;
        }

        Matcher matcher = LINK_PATTERN.matcher(markdownContent);
        Map<String, NoteLink> linkMap = new LinkedHashMap<>();
        while (matcher.find()) {
            String targetName = matcher.group(1).trim();
            if (targetName.isEmpty() || linkMap.containsKey(targetName)) {
                continue;
            }
            Note targetNote = noteMapper.selectOne(new LambdaQueryWrapper<Note>()
                    .eq(Note::getKnowledgeBaseId, note.getKnowledgeBaseId())
                    .eq(Note::getTitle, targetName)
                    .eq(Note::getStatus, 1)
                    .last("limit 1"));

            NoteLink link = new NoteLink();
            link.setKnowledgeBaseId(note.getKnowledgeBaseId());
            link.setSourceNoteId(note.getId());
            link.setTargetNoteId(targetNote == null ? null : targetNote.getId());
            link.setTargetNoteName(targetName);
            link.setAnchorText(targetName);
            link.setContextSnippet(buildSnippet(markdownContent, matcher.start(), matcher.end()));
            link.setIsBroken(targetNote == null ? 1 : 0);
            linkMap.put(targetName, link);
        }

        for (NoteLink link : linkMap.values()) {
            noteLinkMapper.insert(link);
        }
        refreshBrokenLinks(note.getKnowledgeBaseId(), note.getId());
    }

    private void refreshBrokenLinks(Long knowledgeBaseId, Long excludeSourceNoteId) {
        List<Note> relatedNotes = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .eq(Note::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Note::getStatus, 1));
        Map<String, Long> titleMap = new java.util.HashMap<>();
        for (Note relatedNote : relatedNotes) {
            titleMap.put(relatedNote.getTitle(), relatedNote.getId());
        }

        List<NoteLink> links = noteLinkMapper.selectList(new LambdaQueryWrapper<NoteLink>()
                .eq(NoteLink::getKnowledgeBaseId, knowledgeBaseId));
        for (NoteLink link : links) {
            if (Objects.equals(link.getSourceNoteId(), excludeSourceNoteId)) {
                continue;
            }
            Long targetNoteId = titleMap.get(link.getTargetNoteName());
            link.setTargetNoteId(targetNoteId);
            link.setIsBroken(targetNoteId == null ? 1 : 0);
            noteLinkMapper.updateById(link);
        }
    }

    private String buildSnippet(String content, int start, int end) {
        int left = Math.max(0, start - 20);
        int right = Math.min(content.length(), end + 20);
        return content.substring(left, right).replaceAll("\\s+", " ").trim();
    }

    private List<NoteLinkVO> buildLinkVOList(List<NoteLink> links) {
        List<NoteLinkVO> result = new ArrayList<>();
        for (NoteLink link : links) {
            NoteLinkVO vo = new NoteLinkVO();
            BeanUtils.copyProperties(link, vo);
            result.add(vo);
        }
        return result;
    }
}
