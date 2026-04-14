package org.example.dln.controller;

import org.example.dln.service.NoteHistoryService;
import org.example.dln.vo.NoteDetailVO;
import org.example.dln.vo.NoteHistoryDetailVO;
import org.example.dln.vo.NoteHistoryVO;
import org.example.dln.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notes/{noteId}/histories")
public class NoteHistoryController {
    @Autowired
    private NoteHistoryService noteHistoryService;

    @GetMapping
    public Result<List<NoteHistoryVO>> listNoteHistories(@PathVariable Long noteId,
                                                         @RequestAttribute("userId") Long userId) {
        return Result.success(noteHistoryService.listNoteHistories(userId, noteId));
    }

    @GetMapping("/{historyId}")
    public Result<NoteHistoryDetailVO> getNoteHistoryDetail(@PathVariable Long noteId,
                                                            @PathVariable Long historyId,
                                                            @RequestAttribute("userId") Long userId) {
        return Result.success(noteHistoryService.getNoteHistoryDetail(userId, noteId, historyId));
    }

    @PostMapping("/snapshot")
    public Result<NoteHistoryVO> createHistorySnapshot(@PathVariable Long noteId,
                                                       @RequestAttribute("userId") Long userId) {
        return Result.success("创建历史版本成功", noteHistoryService.createHistorySnapshot(userId, noteId));
    }

    @PostMapping("/{historyId}/restore")
    public Result<NoteDetailVO> restoreNoteHistory(@PathVariable Long noteId,
                                                   @PathVariable Long historyId,
                                                   @RequestAttribute("userId") Long userId) {
        return Result.success("恢复历史版本成功", noteHistoryService.restoreNoteHistory(userId, noteId, historyId));
    }
}
