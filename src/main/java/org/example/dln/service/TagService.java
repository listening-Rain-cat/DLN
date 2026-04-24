package org.example.dln.service;

import org.example.dln.dto.CreateTagDTO;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteTag;
import org.example.dln.entity.Tag;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.mapper.NoteTagMapper;
import org.example.dln.mapper.TagMapper;
import org.example.dln.util.LongStringUtils;
import org.example.dln.vo.TagVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 包名：org.example.dln.service
 * 类名：TagService
 * 类描述：处理标签创建、查询、删除和笔记标签绑定逻辑。
 * 创建人：@author Rain_润
 */
@Service
public class TagService {
    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
    * 创建标签。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
     * @param dto 创建标签请求参数
    */
    public TagVO createTag(Long userId, Long knowledgeBaseId, CreateTagDTO dto) {
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        String name = dto.getName().trim();
        checkTagNameExists(knowledgeBaseId, name, null);

        Tag tag = new Tag();
        tag.setKnowledgeBaseId(knowledgeBaseId);
        tag.setName(name);
        if (tagMapper.insert(tag) <= 0) {
            throw new BusinessException("创建标签失败");
        }
        knowledgeBaseService.touchKnowledgeBase(knowledgeBaseId);
        return toTagVO(tag);
    }

    /**
    * 查询知识库标签列表。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
    */
    public List<TagVO> listKnowledgeBaseTags(Long userId, Long knowledgeBaseId) {
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        List<Tag> tags = tagMapper.selectByKnowledgeBaseIdOrderByNameAsc(knowledgeBaseId);
        return buildTagVOList(tags);
    }

    /**
    * 删除标签。
     * @param userId 用户ID
     * @param tagId 标签ID
    */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long userId, Long tagId) {
        Tag tag = getTagByIdOrThrow(tagId);
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, tag.getKnowledgeBaseId());

        noteTagMapper.deleteByTagId(tagId);
        if (tagMapper.deleteById(tagId) <= 0) {
            throw new BusinessException("删除标签失败");
        }
        knowledgeBaseService.touchKnowledgeBase(tag.getKnowledgeBaseId());
    }

    /**
    * 设置笔记标签。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param tagIds 标签ID列表
    */
    @Transactional(rollbackFor = Exception.class)
    public List<TagVO> setNoteTags(Long userId, Long noteId, List<Long> tagIds) {
        Note note = getNoteOrThrow(userId, noteId);
        Set<Long> normalizedTagIds = normalizeTagIds(tagIds);

        noteTagMapper.deleteByNoteId(noteId);
        if (normalizedTagIds.isEmpty()) {
            noteMapper.touchUpdatedTime(noteId);
            knowledgeBaseService.touchKnowledgeBase(note.getKnowledgeBaseId());
            return new ArrayList<>();
        }

        List<Tag> tags = tagMapper.selectByTagIds(normalizedTagIds);
        if (tags.size() != normalizedTagIds.size()) {
            throw new BusinessException("标签不存在");
        }
        for (Tag tag : tags) {
            if (!Objects.equals(tag.getKnowledgeBaseId(), note.getKnowledgeBaseId())) {
                throw new BusinessException("只能绑定当前知识库下的标签");
            }
        }

        for (Long tagId : normalizedTagIds) {
            NoteTag noteTag = new NoteTag();
            noteTag.setNoteId(noteId);
            noteTag.setTagId(tagId);
            noteTagMapper.insert(noteTag);
        }

        noteMapper.touchUpdatedTime(noteId);
        knowledgeBaseService.touchKnowledgeBase(note.getKnowledgeBaseId());
        return buildTagVOList(tags);
    }

    /**
    * 查询笔记标签列表。
     * @param userId 用户ID
     * @param noteId 笔记ID
    */
    public List<TagVO> listNoteTags(Long userId, Long noteId) {
        Note note = getNoteOrThrow(userId, noteId);
        List<NoteTag> noteTags = noteTagMapper.selectByNoteId(noteId);
        if (noteTags.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> tagIds = new LinkedHashSet<>();
        for (NoteTag noteTag : noteTags) {
            tagIds.add(noteTag.getTagId());
        }

        List<Tag> tags = tagMapper.selectByTagIds(tagIds).stream()
                .filter(tag -> Objects.equals(tag.getKnowledgeBaseId(), note.getKnowledgeBaseId()))
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return buildTagVOList(tags);
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
    * 获取标签，不存在时抛出异常。
     * @param tagId 标签ID
    */
    private Tag getTagByIdOrThrow(Long tagId) {
        Tag tag = tagMapper.selectByTagId(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        return tag;
    }

    /**
    * 检查标签名称是否已存在。
     * @param knowledgeBaseId 知识库ID
     * @param name 名称
     * @param excludeId 排除的标签ID（更新时使用，新增时传null）
    */
    private void checkTagNameExists(Long knowledgeBaseId, String name, Long excludeId) {
        List<Tag> tags = tagMapper.selectByKnowledgeBaseIdAndName(knowledgeBaseId, name);
        boolean exists = tags.stream().anyMatch(tag -> !Objects.equals(tag.getId(), excludeId));
        if (exists) {
            throw new BusinessException("标签名称已存在");
        }
    }

    /**
    * 规范化标签 ID 集合。
     * @param tagIds 标签ID列表
    */
    private Set<Long> normalizeTagIds(List<Long> tagIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (tagIds == null) {
            return result;
        }
        for (Long tagId : tagIds) {
            if (tagId != null) {
                result.add(tagId);
            }
        }
        return result;
    }

    /**
    * 构建标签视图对象列表。
     * @param tags 标签列表
    */
    private List<TagVO> buildTagVOList(List<Tag> tags) {
        List<TagVO> result = new ArrayList<>();
        for (Tag tag : tags) {
            result.add(toTagVO(tag));
        }
        result.sort(Comparator.comparing(TagVO::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    /**
    * 将标签实体转换为标签视图对象。
     * @param tag 标签实体
    */
    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        vo.setId(LongStringUtils.toStringValue(tag.getId()));
        vo.setKnowledgeBaseId(LongStringUtils.toStringValue(tag.getKnowledgeBaseId()));
        return vo;
    }
}
