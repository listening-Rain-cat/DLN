package org.example.dln.controller;

import jakarta.validation.Valid;
import org.example.dln.dto.CreateNoteDTO;
import org.example.dln.dto.UpdateNoteDTO;
import org.example.dln.service.NoteService;
import org.example.dln.vo.NoteDetailVO;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 包名：org.example.dln.controller
 * 类名：NoteController
 * 类描述：笔记控制器。
 * 创建人：@author Rain_润
 */
@RestController
@RequestMapping("/notes")
public class NoteController {
    @Autowired
    private NoteService noteService;

    @PostMapping
    public Result<NoteDetailVO> createNote(@Valid @RequestBody CreateNoteDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("创建笔记成功", noteService.createNote(userId, dto));
    }

    @GetMapping("/{noteId}")
    public Result<NoteDetailVO> getNoteDetail(@PathVariable Long noteId,
                                              @RequestAttribute("userId") Long userId) {
        return Result.success(noteService.getNoteDetail(userId, noteId));
    }

    @PutMapping("/{noteId}")
    public Result<NoteDetailVO> updateNote(@PathVariable Long noteId,
                                           @Valid @RequestBody UpdateNoteDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("更新笔记成功", noteService.updateNote(userId, noteId, dto));
    }

    @DeleteMapping("/{noteId}")
    public Result<Void> deleteNote(@PathVariable Long noteId,
                                   @RequestAttribute("userId") Long userId) {
        noteService.deleteNote(userId, noteId);
        return Result.success("删除笔记成功", null);
    }
}
