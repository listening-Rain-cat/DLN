package org.example.dln.service;

import org.example.dln.dto.CreateTagDTO;
import org.example.dln.entity.KnowledgeBase;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteTag;
import org.example.dln.entity.Tag;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.mapper.NoteTagMapper;
import org.example.dln.mapper.TagMapper;
import org.example.dln.vo.TagVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 包名：org.example.dln.service
 * 类名：TagServiceTest
 * 类描述：测试标签服务相关业务逻辑。
 * 创建人：@author Rain_润
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long KNOWLEDGE_BASE_ID = 10L;
    private static final Long NOTE_ID = 100L;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private NoteTagMapper noteTagMapper;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private TagService tagService;

    @Test
    void createTagShouldTrimNameAndTouchKnowledgeBase() {
        CreateTagDTO dto = new CreateTagDTO();
        dto.setName("  Beta  ");

        when(knowledgeBaseService.getKnowledgeBaseOrThrow(USER_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(buildKnowledgeBase(KNOWLEDGE_BASE_ID, USER_ID));
        when(tagMapper.selectByKnowledgeBaseIdAndName(KNOWLEDGE_BASE_ID, "Beta"))
                .thenReturn(List.of());
        when(tagMapper.insert(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(200L);
            return 1;
        });

        TagVO result = tagService.createTag(USER_ID, KNOWLEDGE_BASE_ID, dto);

        assertEquals(200L, result.getId());
        assertEquals(KNOWLEDGE_BASE_ID, result.getKnowledgeBaseId());
        assertEquals("Beta", result.getName());
        verify(knowledgeBaseService).touchKnowledgeBase(KNOWLEDGE_BASE_ID);
    }

    @Test
    void setNoteTagsShouldDeduplicatePersistAndReturnSortedTags() {
        Note note = buildNote(NOTE_ID, USER_ID, KNOWLEDGE_BASE_ID);
        Tag beta = buildTag(2L, KNOWLEDGE_BASE_ID, "Beta");
        Tag alpha = buildTag(1L, KNOWLEDGE_BASE_ID, "Alpha");

        when(noteMapper.selectByNoteId(NOTE_ID)).thenReturn(note);
        when(knowledgeBaseService.getKnowledgeBaseOrThrow(USER_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(buildKnowledgeBase(KNOWLEDGE_BASE_ID, USER_ID));
        when(tagMapper.selectByTagIds(any())).thenReturn(List.of(beta, alpha));

        List<TagVO> result = tagService.setNoteTags(USER_ID, NOTE_ID, Arrays.asList(2L, null, 1L, 2L));

        ArgumentCaptor<NoteTag> noteTagCaptor = ArgumentCaptor.forClass(NoteTag.class);
        verify(noteTagMapper, times(2)).insert(noteTagCaptor.capture());
        assertIterableEquals(
                List.of(2L, 1L),
                noteTagCaptor.getAllValues().stream().map(NoteTag::getTagId).toList()
        );
        assertIterableEquals(List.of("Alpha", "Beta"), result.stream().map(TagVO::getName).toList());
        verify(noteTagMapper).deleteByNoteId(NOTE_ID);
        verify(knowledgeBaseService).touchKnowledgeBase(KNOWLEDGE_BASE_ID);
    }

    @Test
    void setNoteTagsShouldTouchKnowledgeBaseWhenClearingAllTags() {
        Note note = buildNote(NOTE_ID, USER_ID, KNOWLEDGE_BASE_ID);

        when(noteMapper.selectByNoteId(NOTE_ID)).thenReturn(note);
        when(knowledgeBaseService.getKnowledgeBaseOrThrow(USER_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(buildKnowledgeBase(KNOWLEDGE_BASE_ID, USER_ID));

        List<TagVO> result = tagService.setNoteTags(USER_ID, NOTE_ID, List.of());

        assertEquals(0, result.size());
        verify(noteTagMapper).deleteByNoteId(NOTE_ID);
        verify(noteTagMapper, never()).insert(any(NoteTag.class));
        verify(knowledgeBaseService).touchKnowledgeBase(KNOWLEDGE_BASE_ID);
    }

    @Test
    void setNoteTagsShouldRejectTagsFromAnotherKnowledgeBase() {
        Note note = buildNote(NOTE_ID, USER_ID, KNOWLEDGE_BASE_ID);
        Tag wrongTag = buildTag(9L, KNOWLEDGE_BASE_ID + 1, "CrossKb");

        when(noteMapper.selectByNoteId(NOTE_ID)).thenReturn(note);
        when(knowledgeBaseService.getKnowledgeBaseOrThrow(USER_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(buildKnowledgeBase(KNOWLEDGE_BASE_ID, USER_ID));
        when(tagMapper.selectByTagIds(any())).thenReturn(List.of(wrongTag));

        assertThrows(BusinessException.class, () -> tagService.setNoteTags(USER_ID, NOTE_ID, List.of(9L)));

        verify(noteTagMapper).deleteByNoteId(NOTE_ID);
        verify(noteTagMapper, never()).insert(any(NoteTag.class));
        verify(knowledgeBaseService, never()).touchKnowledgeBase(KNOWLEDGE_BASE_ID);
    }

    @Test
    void listNoteTagsShouldFilterOtherKnowledgeBaseTagsAndSort() {
        Note note = buildNote(NOTE_ID, USER_ID, KNOWLEDGE_BASE_ID);
        NoteTag noteTagOne = new NoteTag();
        noteTagOne.setTagId(2L);
        NoteTag noteTagTwo = new NoteTag();
        noteTagTwo.setTagId(1L);
        Tag beta = buildTag(2L, KNOWLEDGE_BASE_ID, "Beta");
        Tag ignored = buildTag(3L, KNOWLEDGE_BASE_ID + 1, "Ignored");
        Tag alpha = buildTag(1L, KNOWLEDGE_BASE_ID, "Alpha");

        when(noteMapper.selectByNoteId(NOTE_ID)).thenReturn(note);
        when(knowledgeBaseService.getKnowledgeBaseOrThrow(USER_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(buildKnowledgeBase(KNOWLEDGE_BASE_ID, USER_ID));
        when(noteTagMapper.selectByNoteId(NOTE_ID)).thenReturn(List.of(noteTagOne, noteTagTwo));
        when(tagMapper.selectByTagIds(any())).thenReturn(List.of(beta, ignored, alpha));

        List<TagVO> result = tagService.listNoteTags(USER_ID, NOTE_ID);

        assertIterableEquals(List.of("Alpha", "Beta"), result.stream().map(TagVO::getName).toList());
    }

    @Test
    void deleteTagShouldRemoveAssociationsAndTouchKnowledgeBase() {
        Tag tag = buildTag(5L, KNOWLEDGE_BASE_ID, "ToDelete");

        when(tagMapper.selectByTagId(5L)).thenReturn(tag);
        when(knowledgeBaseService.getKnowledgeBaseOrThrow(USER_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(buildKnowledgeBase(KNOWLEDGE_BASE_ID, USER_ID));
        when(tagMapper.deleteById(5L)).thenReturn(1);

        tagService.deleteTag(USER_ID, 5L);

        verify(noteTagMapper).deleteByTagId(5L);
        verify(tagMapper).deleteById(5L);
        verify(knowledgeBaseService).touchKnowledgeBase(KNOWLEDGE_BASE_ID);
    }

    private KnowledgeBase buildKnowledgeBase(Long id, Long userId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setUserId(userId);
        knowledgeBase.setStatus(1);
        return knowledgeBase;
    }

    private Note buildNote(Long noteId, Long userId, Long knowledgeBaseId) {
        Note note = new Note();
        note.setId(noteId);
        note.setUserId(userId);
        note.setKnowledgeBaseId(knowledgeBaseId);
        note.setStatus(1);
        return note;
    }

    private Tag buildTag(Long id, Long knowledgeBaseId, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setKnowledgeBaseId(knowledgeBaseId);
        tag.setName(name);
        return tag;
    }
}
