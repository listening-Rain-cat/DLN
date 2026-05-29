package org.example.dln.controller;

import org.example.dln.security.CurrentUserId;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 包名：org.example.dln.controller
 * 类名：TagController
 * 类描述：提供标签创建、查询、删除和笔记标签绑定相关接口。
 * 创建人：@author Rain_润
 */
@RestController
@RequestMapping
public class TagController {
    @Autowired
    private TagService tagService;

    /**
    * 创建标签。
     * @param knowledgeBaseId 知识库ID
     * @param dto 创建标签请求参数
     * @param userId 用户ID
    */
    @PostMapping("/knowledgeBases/{knowledgeBaseId}/tags")
    public Result<TagVO> createTag(@PathVariable Long knowledgeBaseId,
                                   @Valid @RequestBody CreateTagDTO dto,
                                   @CurrentUserId Long userId) {
        return Result.success("创建标签成功", tagService.createTag(userId, knowledgeBaseId, dto));
    }

    /**
    * 查询知识库标签列表。
     * @param knowledgeBaseId 知识库ID
     * @param userId 用户ID
    */
    @GetMapping("/knowledgeBases/{knowledgeBaseId}/tags")
    public Result<List<TagVO>> listKnowledgeBaseTags(@PathVariable Long knowledgeBaseId,
                                                     @CurrentUserId Long userId) {
        return Result.success(tagService.listKnowledgeBaseTags(userId, knowledgeBaseId));
    }

    /**
    * 删除标签。
     * @param tagId 标签ID
     * @param userId 用户ID
    */
    @DeleteMapping("/tags/{tagId}")
    public Result<Void> deleteTag(@PathVariable Long tagId,
                                  @CurrentUserId Long userId) {
        tagService.deleteTag(userId, tagId);
        return Result.success("删除标签成功", null);
    }

    /**
    * 查询笔记标签列表。
     * @param noteId 笔记ID
     * @param userId 用户ID
    */
    @GetMapping("/notes/{noteId}/tags")
    public Result<List<TagVO>> listNoteTags(@PathVariable Long noteId,
                                            @CurrentUserId Long userId) {
        return Result.success(tagService.listNoteTags(userId, noteId));
    }

    /**
    * 设置笔记标签。
     * @param noteId 笔记ID
     * @param dto 笔记标签设置参数
     * @param userId 用户ID
    */
    @PutMapping("/notes/{noteId}/tags")
    public Result<List<TagVO>> setNoteTags(@PathVariable Long noteId,
                                           @RequestBody SetNoteTagsDTO dto,
                                           @CurrentUserId Long userId) {
        return Result.success("设置笔记标签成功", tagService.setNoteTags(userId, noteId, dto.getTagIds()));
    }
}
