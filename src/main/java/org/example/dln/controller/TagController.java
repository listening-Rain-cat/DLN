package org.example.dln.controller;

import jakarta.validation.Valid;
import org.example.dln.dto.CreateTagDTO;
import org.example.dln.dto.SetNoteTagsDTO;
import org.example.dln.service.TagService;
import org.example.dln.vo.Result;
import org.example.dln.vo.TagVO;
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

import java.util.List;

/**
 * 包名：org.example.dln.controller
 * 类名：TagController
 * 类描述：标签控制器。
 * 创建人：@author Rain_润
 */
@RestController
@RequestMapping
public class TagController {
    @Autowired
    private TagService tagService;

    @PostMapping("/knowledgeBases/{knowledgeBaseId}/tags")
    public Result<TagVO> createTag(@PathVariable Long knowledgeBaseId,
                                   @Valid @RequestBody CreateTagDTO dto,
                                   @RequestAttribute("userId") Long userId) {
        return Result.success("创建标签成功", tagService.createTag(userId, knowledgeBaseId, dto));
    }

    @GetMapping("/knowledgeBases/{knowledgeBaseId}/tags")
    public Result<List<TagVO>> listKnowledgeBaseTags(@PathVariable Long knowledgeBaseId,
                                                     @RequestAttribute("userId") Long userId) {
        return Result.success(tagService.listKnowledgeBaseTags(userId, knowledgeBaseId));
    }

    @DeleteMapping("/tags/{tagId}")
    public Result<Void> deleteTag(@PathVariable Long tagId,
                                  @RequestAttribute("userId") Long userId) {
        tagService.deleteTag(userId, tagId);
        return Result.success("删除标签成功", null);
    }

    @GetMapping("/notes/{noteId}/tags")
    public Result<List<TagVO>> listNoteTags(@PathVariable Long noteId,
                                            @RequestAttribute("userId") Long userId) {
        return Result.success(tagService.listNoteTags(userId, noteId));
    }

    @PutMapping("/notes/{noteId}/tags")
    public Result<List<TagVO>> setNoteTags(@PathVariable Long noteId,
                                           @RequestBody SetNoteTagsDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("设置笔记标签成功", tagService.setNoteTags(userId, noteId, dto.getTagIds()));
    }
}
