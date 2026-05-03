package org.example.dln.service;

import org.example.dln.dto.AutoSaveNoteContentDTO;
import org.example.dln.dto.AutoSaveNoteTitleDTO;
import org.example.dln.dto.CreateNoteDTO;
import org.example.dln.dto.UpdateNoteDTO;
import org.example.dln.entity.Folder;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteContent;
import org.example.dln.entity.NoteHistory;
import org.example.dln.entity.NoteLink;
import org.example.dln.entity.NoteTemplate;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteContentMapper;
import org.example.dln.mapper.NoteHistoryMapper;
import org.example.dln.mapper.NoteLinkMapper;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.mapper.NoteTagMapper;
import org.example.dln.util.LongStringUtils;
import org.example.dln.vo.NoteDetailVO;
import org.example.dln.vo.NoteLinkCandidateVO;
import org.example.dln.vo.NoteLinkVO;
import org.example.dln.vo.NoteLinkPreviewVO;
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
 * 包名：org.example.dln.service
 * 类名：NoteService
 * 类描述：处理笔记创建、编辑、详情查询、双链和附件关联逻辑。
 * 创建人：@author Rain_润
 */
@Service
public class NoteService {
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[([^\\[\\]]+)]]");
    private static final int LINK_CANDIDATE_LIMIT = 30;
    private static final int LINK_PREVIEW_MAX_LENGTH = 1600;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteContentMapper noteContentMapper;

    @Autowired
    private NoteHistoryMapper noteHistoryMapper;

    @Autowired
    private NoteLinkMapper noteLinkMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private TagService tagService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private NoteTemplateService noteTemplateService;

    @Autowired
    private NoteLinkCandidateCacheService noteLinkCandidateCacheService;

    /**
    * 创建笔记。
     * @param userId 用户ID
     * @param dto 创建笔记请求参数
    */
    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO createNote(Long userId, CreateNoteDTO dto) {
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, dto.getKnowledgeBaseId());
        validateFolder(dto.getKnowledgeBaseId(), dto.getFolderId());

        String title = dto.getTitle().trim();
        checkNoteTitleExists(dto.getKnowledgeBaseId(), title, null);
        String markdownContent = resolveInitialMarkdownContent(userId, dto);

        Note note = new Note();
        note.setUserId(userId);
        note.setKnowledgeBaseId(dto.getKnowledgeBaseId());
        note.setFolderId(dto.getFolderId());
        note.setTitle(title);
        note.setStatus(1);
        note.setDeleteToken(0L);
        if (noteMapper.insert(note) <= 0) {
            throw new BusinessException("创建笔记失败");
        }

        NoteContent noteContent = new NoteContent();
        noteContent.setNoteId(note.getId());
        noteContent.setMarkdownContent(markdownContent);
        if (noteContentMapper.insert(noteContent) <= 0) {
            throw new BusinessException("保存笔记内容失败");
        }

        refreshLinks(note, markdownContent);
        knowledgeBaseService.touchKnowledgeBase(note.getKnowledgeBaseId());
        return getNoteDetail(userId, note.getId());
    }

    /**
    * 获取笔记详情。
     * @param userId 用户ID
     * @param noteId 笔记ID
    */
    public NoteDetailVO getNoteDetail(Long userId, Long noteId) {
        Note note = getNoteOrThrow(userId, noteId);
        NoteContent noteContent = noteContentMapper.selectByNoteId(noteId);

        NoteDetailVO vo = new NoteDetailVO();
        BeanUtils.copyProperties(note, vo);
        vo.setId(LongStringUtils.toStringValue(note.getId()));
        vo.setKnowledgeBaseId(LongStringUtils.toStringValue(note.getKnowledgeBaseId()));
        vo.setFolderId(LongStringUtils.toStringValue(note.getFolderId()));
        vo.setMarkdownContent(noteContent == null ? null : noteContent.getMarkdownContent());
        vo.setTags(tagService.listNoteTags(userId, noteId));
        vo.setAttachments(attachmentService.listNoteAttachments(userId, noteId));
        vo.setOutgoingLinks(buildLinkVOList(noteLinkMapper.selectOutgoingByNoteId(noteId)));
        vo.setIncomingLinks(buildLinkVOList(noteLinkMapper.selectIncomingByNoteId(noteId)));
        return vo;
    }

    /**
    * 查询链接候选项列表。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param keyword 检索关键词
    */
    public List<NoteLinkCandidateVO> listLinkCandidates(Long userId, Long noteId, String keyword) {
        Note note = getNoteOrThrow(userId, noteId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        List<NoteLinkCandidateVO> candidates = noteLinkCandidateCacheService.listKnowledgeBaseCandidates(
                userId,
                note.getKnowledgeBaseId()
        );

        List<NoteLinkCandidateVO> result = new ArrayList<>();
        for (NoteLinkCandidateVO candidate : candidates) {
            String title = candidate.getTitle();
            if (title == null || title.isBlank()) {
                continue;
            }

            if (!normalizedKeyword.isEmpty() && !title.toLowerCase().contains(normalizedKeyword)) {
                continue;
            }

            result.add(candidate);

            if (result.size() >= LINK_CANDIDATE_LIMIT) {
                break;
            }
        }

        return result;
    }

    /**
    * 获取双链预览内容。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param title 标题
    */
    public NoteLinkPreviewVO getLinkPreview(Long userId, Long noteId, String title) {
        Note sourceNote = getNoteOrThrow(userId, noteId);
        String targetTitle = title == null ? "" : title.trim();
        if (targetTitle.isEmpty()) {
            throw new BusinessException("双链目标标题不能为空");
        }

        NoteLinkPreviewVO preview = new NoteLinkPreviewVO();
        preview.setTitle(targetTitle);

        Note targetNote = noteMapper.selectActiveFirstByKnowledgeBaseIdAndTitle(
                sourceNote.getKnowledgeBaseId(),
                targetTitle
        );

        if (targetNote == null) {
            preview.setIsBroken(1);
            preview.setMarkdownContent("");
            return preview;
        }

        NoteContent noteContent = noteContentMapper.selectByNoteId(targetNote.getId());
        preview.setNoteId(LongStringUtils.toStringValue(targetNote.getId()));
        preview.setTitle(targetNote.getTitle());
        preview.setIsBroken(0);
        preview.setMarkdownContent(buildPreviewContent(noteContent == null ? null : noteContent.getMarkdownContent()));
        return preview;
    }

    /**
    * 更新笔记。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param dto 更新笔记请求参数
    */
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

        NoteContent noteContent = noteContentMapper.selectByNoteId(noteId);
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

        refreshLinks(note, dto.getMarkdownContent());
        knowledgeBaseService.touchKnowledgeBase(note.getKnowledgeBaseId());
        return getNoteDetail(userId, noteId);
    }

    /**
    * 自动保存笔记正文。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param dto 自动保存笔记正文参数
    */
    @Transactional(rollbackFor = Exception.class)
    public void autoSaveNoteContent(Long userId, Long noteId, AutoSaveNoteContentDTO dto) {
        Note note = getNoteOrThrow(userId, noteId);
        String markdownContent = dto == null ? null : dto.getMarkdownContent();
        NoteContent noteContent = noteContentMapper.selectByNoteId(noteId);
        String persistedContent = noteContent == null ? null : noteContent.getMarkdownContent();

        if (Objects.equals(persistedContent, markdownContent)) {
            return;
        }

        if (noteContent == null) {
            noteContent = new NoteContent();
            noteContent.setNoteId(noteId);
            noteContent.setMarkdownContent(markdownContent);
            if (noteContentMapper.insert(noteContent) <= 0) {
                throw new BusinessException("保存笔记正文失败");
            }
        } else {
            noteContent.setMarkdownContent(markdownContent);
            if (noteContentMapper.updateById(noteContent) <= 0) {
                throw new BusinessException("更新笔记正文失败");
            }
        }

        refreshLinks(note, markdownContent);
        if (noteMapper.touchUpdatedTime(noteId) <= 0) {
            throw new BusinessException("更新笔记时间失败");
        }
        knowledgeBaseService.touchKnowledgeBase(note.getKnowledgeBaseId());
    }

    /**
    * 自动保存笔记标题。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param dto 自动保存笔记标题参数
    */
    @Transactional(rollbackFor = Exception.class)
    public void autoSaveNoteTitle(Long userId, Long noteId, AutoSaveNoteTitleDTO dto) {
        Note note = getNoteOrThrow(userId, noteId);
        String rawTitle = dto == null ? null : dto.getTitle();

        if (rawTitle == null || rawTitle.trim().isEmpty()) {
            throw new BusinessException("笔记标题不能为空");
        }

        String title = rawTitle.trim();
        if (Objects.equals(note.getTitle(), title)) {
            return;
        }

        checkNoteTitleExists(note.getKnowledgeBaseId(), title, noteId);
        note.setTitle(title);
        if (noteMapper.updateById(note) <= 0) {
            throw new BusinessException("更新笔记标题失败");
        }

        refreshBrokenLinks(note.getKnowledgeBaseId(), noteId);
        knowledgeBaseService.touchKnowledgeBase(note.getKnowledgeBaseId());
    }

    /**
    * 删除笔记。
     * @param userId 用户ID
     * @param noteId 笔记ID
    */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long userId, Long noteId) {
        Note note = getNoteOrThrow(userId, noteId);
        LocalDateTime deletedTime = LocalDateTime.now();
        note.setStatus(2);
        note.setDeletedTime(deletedTime);
        note.setDeleteToken(note.getId());
        if (noteMapper.updateById(note) <= 0) {
            throw new BusinessException("删除笔记失败");
        }
        noteLinkMapper.deleteBySourceNoteId(noteId);
        noteTagMapper.deleteByNoteId(noteId);
        refreshBrokenLinks(note.getKnowledgeBaseId(), noteId);
        knowledgeBaseService.touchKnowledgeBase(note.getKnowledgeBaseId());
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
    * 校验文件夹是否合法。
     * @param knowledgeBaseId 知识库ID
     * @param folderId 文件夹ID
    */
    private void validateFolder(Long knowledgeBaseId, Long folderId) {
        if (folderId == null) {
            return;
        }

        Folder folder = knowledgeBaseService.getFolderInKnowledgeBaseOrThrow(knowledgeBaseId, folderId);
        if (folder.getStatus() == null || folder.getStatus() != 1) {
            throw new BusinessException("文件夹不存在");
        }
    }

    /**
    * 检查笔记标题是否已存在。
     * @param knowledgeBaseId 知识库ID
     * @param title 标题
     * @param excludeId 排除的笔记ID（更新时使用，新增时传null）
    */
    private void checkNoteTitleExists(Long knowledgeBaseId, String title, Long excludeId) {
        List<Note> notes = noteMapper.selectActiveByKnowledgeBaseIdAndTitle(knowledgeBaseId, title);
        boolean exists = notes.stream().anyMatch(note -> !Objects.equals(note.getId(), excludeId));
        if (exists) {
            throw new BusinessException("当前知识库下已存在同名笔记");
        }
    }

    /**
    * 保存笔记历史版本。
     * @param note 笔记实体
     * @param markdownContent Markdown内容
     * @param userId 用户ID
    */
    private void saveHistory(Note note, String markdownContent, Long userId) {
        NoteHistory latest = noteHistoryMapper.selectLatestByNoteId(note.getId());
        if (latest != null
                && Objects.equals(latest.getTitle(), note.getTitle())
                && Objects.equals(latest.getMarkdownContent(), markdownContent)) {
            return;
        }

        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;

        NoteHistory history = new NoteHistory();
        history.setNoteId(note.getId());
        history.setVersionNo(versionNo);
        history.setTitle(note.getTitle());
        history.setMarkdownContent(markdownContent);
        history.setCreatedBy(userId);
        if (noteHistoryMapper.insert(history) <= 0) {
            throw new BusinessException("保存笔记历史版本失败");
        }
    }

    /**
    * 刷新笔记双链关系。
     * @param note 笔记实体
     * @param markdownContent Markdown内容
    */
    private void refreshLinks(Note note, String markdownContent) {
        noteLinkMapper.deleteBySourceNoteId(note.getId());
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

            Note targetNote = noteMapper.selectActiveFirstByKnowledgeBaseIdAndTitle(note.getKnowledgeBaseId(), targetName);

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

    /**
    * 刷新失效双链状态。
     * @param knowledgeBaseId 知识库ID
     * @param excludeSourceNoteId 排除的源笔记ID
    */
    private void refreshBrokenLinks(Long knowledgeBaseId, Long excludeSourceNoteId) {
        List<Note> relatedNotes = noteMapper.selectActiveByKnowledgeBaseId(knowledgeBaseId);
        Map<String, Long> titleMap = new java.util.HashMap<>();
        for (Note relatedNote : relatedNotes) {
            titleMap.put(relatedNote.getTitle(), relatedNote.getId());
        }

        List<NoteLink> links = noteLinkMapper.selectByKnowledgeBaseId(knowledgeBaseId);
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

    /**
    * 构建内容摘要片段。
     * @param content 内容文本
     * @param start 起始位置
     * @param end 结束位置
    */
    private String buildSnippet(String content, int start, int end) {
        int left = Math.max(0, start - 20);
        int right = Math.min(content.length(), end + 20);
        return content.substring(left, right).replaceAll("\\s+", " ").trim();
    }

    /**
    * 构建预览内容。
     * @param markdownContent Markdown内容
    */
    private String buildPreviewContent(String markdownContent) {
        if (markdownContent == null) {
            return "";
        }

        String trimmed = markdownContent.trim();
        if (trimmed.length() <= LINK_PREVIEW_MAX_LENGTH) {
            return trimmed;
        }

        return trimmed.substring(0, LINK_PREVIEW_MAX_LENGTH) + "\n\n...";
    }

    /**
    * 解析初始 Markdown 内容。
     * @param userId 用户ID
     * @param dto 创建笔记请求参数
    */
    private String resolveInitialMarkdownContent(Long userId, CreateNoteDTO dto) {
        if (dto.getTemplateId() == null) {
            return dto.getMarkdownContent();
        }

        NoteTemplate template = noteTemplateService.getTemplateOrThrow(userId, dto.getTemplateId());
        return template.getTemplateContent();
    }

    /**
    * 构建链接视图对象列表。
     * @param links 双链列表
    */
    private List<NoteLinkVO> buildLinkVOList(List<NoteLink> links) {
        List<NoteLinkVO> result = new ArrayList<>();
        for (NoteLink link : links) {
            NoteLinkVO vo = new NoteLinkVO();
            BeanUtils.copyProperties(link, vo);
            vo.setId(LongStringUtils.toStringValue(link.getId()));
            vo.setSourceNoteId(LongStringUtils.toStringValue(link.getSourceNoteId()));
            vo.setTargetNoteId(LongStringUtils.toStringValue(link.getTargetNoteId()));
            result.add(vo);
        }
        return result;
    }
}
