package org.example.dln.controller;

import jakarta.validation.Valid;
import org.example.dln.dto.AutoSaveNoteContentDTO;
import org.example.dln.dto.AutoSaveNoteTitleDTO;
import org.example.dln.dto.CreateNoteDTO;
import org.example.dln.dto.UpdateNoteDTO;
import org.example.dln.service.NoteService;
import org.example.dln.vo.NoteDetailVO;
import org.example.dln.vo.NoteLinkCandidateVO;
import org.example.dln.vo.NoteLinkPreviewVO;
import org.example.dln.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 包名：org.example.dln.controller
 * 类名：NoteController
 * 类描述：提供笔记创建、查询、编辑和关联操作接口。
 * 创建人：@author Rain_润
 */
@RestController
@RequestMapping("/notes")
public class NoteController {
    @Autowired
    private NoteService noteService;

    /**
    * 创建笔记。
    */
    @PostMapping
    public Result<NoteDetailVO> createNote(@Valid @RequestBody CreateNoteDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("创建笔记成功", noteService.createNote(userId, dto));
    }

    /**
    * 获取笔记详情。
    */
    @GetMapping("/{noteId}")
    public Result<NoteDetailVO> getNoteDetail(@PathVariable Long noteId,
                                              @RequestAttribute("userId") Long userId) {
        return Result.success(noteService.getNoteDetail(userId, noteId));
    }

    /**
    * 查询链接候选项列表。
    */
    @GetMapping("/{noteId}/links/candidates")
    public Result<List<NoteLinkCandidateVO>> listLinkCandidates(@PathVariable Long noteId,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestAttribute("userId") Long userId) {
        return Result.success(noteService.listLinkCandidates(userId, noteId, keyword));
    }

    /**
    * 获取双链预览内容。
    */
    @GetMapping("/{noteId}/links/preview")
    public Result<NoteLinkPreviewVO> getLinkPreview(@PathVariable Long noteId,
                                                    @RequestParam String title,
                                                    @RequestAttribute("userId") Long userId) {
        return Result.success(noteService.getLinkPreview(userId, noteId, title));
    }

    /**
    * 更新笔记。
    */
    @PutMapping("/{noteId}")
    public Result<NoteDetailVO> updateNote(@PathVariable Long noteId,
                                           @Valid @RequestBody UpdateNoteDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("更新笔记成功", noteService.updateNote(userId, noteId, dto));
    }

    /**
    * 自动保存笔记正文。
    */
    @PutMapping("/{noteId}/content/autosave")
    public Result<Void> autoSaveNoteContent(@PathVariable Long noteId,
                                            @RequestBody AutoSaveNoteContentDTO dto,
                                            @RequestAttribute("userId") Long userId) {
        noteService.autoSaveNoteContent(userId, noteId, dto);
        return Result.success("自动保存笔记正文成功", null);
    }

    /**
    * 自动保存笔记标题。
    */
    @PutMapping("/{noteId}/title/autosave")
    public Result<Void> autoSaveNoteTitle(@PathVariable Long noteId,
                                          @RequestBody AutoSaveNoteTitleDTO dto,
                                          @RequestAttribute("userId") Long userId) {
        noteService.autoSaveNoteTitle(userId, noteId, dto);
        return Result.success("自动保存笔记标题成功", null);
    }

    /**
    * 删除笔记。
    */
    @DeleteMapping("/{noteId}")
    public Result<Void> deleteNote(@PathVariable Long noteId,
                                   @RequestAttribute("userId") Long userId) {
        noteService.deleteNote(userId, noteId);
        return Result.success("删除笔记成功", null);
    }
}
