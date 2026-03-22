package org.example.dln.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.dln.dto.CreateTagDTO;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteTag;
import org.example.dln.entity.Tag;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.mapper.NoteTagMapper;
import org.example.dln.mapper.TagMapper;
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
        return toTagVO(tag);
    }

    public List<TagVO> listKnowledgeBaseTags(Long userId, Long knowledgeBaseId) {
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getKnowledgeBaseId, knowledgeBaseId)
                .orderByAsc(Tag::getName));
        return buildTagVOList(tags);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long userId, Long tagId) {
        Tag tag = getTagByIdOrThrow(tagId);
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, tag.getKnowledgeBaseId());

        noteTagMapper.delete(new LambdaQueryWrapper<NoteTag>()
                .eq(NoteTag::getTagId, tagId));
        if (tagMapper.deleteById(tagId) <= 0) {
            throw new BusinessException("删除标签失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public List<TagVO> setNoteTags(Long userId, Long noteId, List<Long> tagIds) {
        Note note = getNoteOrThrow(userId, noteId);
        Set<Long> normalizedTagIds = normalizeTagIds(tagIds);

        noteTagMapper.delete(new LambdaQueryWrapper<NoteTag>()
                .eq(NoteTag::getNoteId, noteId));
        if (normalizedTagIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tag> tags = tagMapper.selectByIds(normalizedTagIds);
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
        return buildTagVOList(tags);
    }

    public List<TagVO> listNoteTags(Long userId, Long noteId) {
        Note note = getNoteOrThrow(userId, noteId);
        List<NoteTag> noteTags = noteTagMapper.selectList(new LambdaQueryWrapper<NoteTag>()
                .eq(NoteTag::getNoteId, noteId));
        if (noteTags.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> tagIds = new LinkedHashSet<>();
        for (NoteTag noteTag : noteTags) {
            tagIds.add(noteTag.getTagId());
        }

        List<Tag> tags = tagMapper.selectByIds(tagIds).stream()
                .filter(tag -> Objects.equals(tag.getKnowledgeBaseId(), note.getKnowledgeBaseId()))
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return buildTagVOList(tags);
    }

    private Note getNoteOrThrow(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || !Objects.equals(note.getUserId(), userId) || note.getStatus() == null || note.getStatus() != 1) {
            throw new BusinessException("笔记不存在或无权限访问");
        }
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, note.getKnowledgeBaseId());
        return note;
    }

    private Tag getTagByIdOrThrow(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        return tag;
    }

    private void checkTagNameExists(Long knowledgeBaseId, String name, Long excludeId) {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Tag::getName, name));
        boolean exists = tags.stream().anyMatch(tag -> !Objects.equals(tag.getId(), excludeId));
        if (exists) {
            throw new BusinessException("标签名称已存在");
        }
    }

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

    private List<TagVO> buildTagVOList(List<Tag> tags) {
        List<TagVO> result = new ArrayList<>();
        for (Tag tag : tags) {
            result.add(toTagVO(tag));
        }
        result.sort(Comparator.comparing(TagVO::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        return vo;
    }
}
