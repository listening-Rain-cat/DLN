package org.example.dln.controller;

import jakarta.validation.Valid;
import org.example.dln.dto.CreateFolderDTO;
import org.example.dln.dto.CreateKnowledgeBaseDTO;
import org.example.dln.dto.UpdateFolderDTO;
import org.example.dln.dto.UpdateKnowledgeBaseDTO;
import org.example.dln.service.KnowledgeBaseService;
import org.example.dln.vo.KnowledgeBaseVO;
import org.example.dln.vo.KnowledgeGraphVO;
import org.example.dln.vo.NoteSearchResultVO;
import org.example.dln.vo.Result;
import org.example.dln.vo.TreeNodeVO;
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
 * 类名：KnowledgeBaseController
 * 类描述：提供知识库创建、查询、更新和删除相关接口。
 * 创建人：@author Rain_润
 */
@RestController
@RequestMapping("/knowledgeBases")
public class KnowledgeBaseController {
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库。
     * @param dto 创建知识库请求参数
     * @param userId 用户ID
     */
    @PostMapping
    public Result<KnowledgeBaseVO> createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseDTO dto,
                                                       @RequestAttribute("userId") Long userId) {
        return Result.success("创建知识库成功", knowledgeBaseService.createKnowledgeBase(userId, dto));
    }

    /**
     * 查询知识库列表。
     * @param userId 用户ID
     */
    @GetMapping
    public Result<List<KnowledgeBaseVO>> listKnowledgeBases(@RequestAttribute("userId") Long userId) {
        return Result.success(knowledgeBaseService.listKnowledgeBases(userId));
    }

    /**
     * 更新知识库。
     * @param knowledgeBaseId 知识库ID
     * @param dto 更新知识库请求参数
     * @param userId 用户ID
     */
    @PutMapping("/{knowledgeBaseId}")
    public Result<KnowledgeBaseVO> updateKnowledgeBase(@PathVariable Long knowledgeBaseId,
                                                       @Valid @RequestBody UpdateKnowledgeBaseDTO dto,
                                                       @RequestAttribute("userId") Long userId) {
        return Result.success("更新知识库成功",
                knowledgeBaseService.updateKnowledgeBase(userId, knowledgeBaseId, dto));
    }

    /**
     * 删除知识库。
     * @param knowledgeBaseId 知识库ID
     * @param userId 用户ID
     */
    @DeleteMapping("/{knowledgeBaseId}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long knowledgeBaseId,
                                            @RequestAttribute("userId") Long userId) {
        knowledgeBaseService.deleteKnowledgeBase(userId, knowledgeBaseId);
        return Result.success("删除知识库成功", null);
    }

    /**
     * 获取知识库目录树。
     * @param knowledgeBaseId 知识库ID
     * @param userId 用户ID
     */
    @GetMapping("/{knowledgeBaseId}/tree")
    public Result<List<TreeNodeVO>> getKnowledgeBaseTree(@PathVariable Long knowledgeBaseId,
                                                         @RequestAttribute("userId") Long userId) {
        return Result.success(knowledgeBaseService.getKnowledgeBaseTree(userId, knowledgeBaseId));
    }

    /**
     * 获取知识图谱。
     * @param knowledgeBaseId 知识库ID
     * @param userId 用户ID
     */
    @GetMapping("/{knowledgeBaseId}/graph")
    public Result<KnowledgeGraphVO> getKnowledgeGraph(@PathVariable Long knowledgeBaseId,
                                                      @RequestAttribute("userId") Long userId) {
        return Result.success(knowledgeBaseService.getKnowledgeGraph(userId, knowledgeBaseId));
    }

    /**
     * 检索知识库内笔记。
     * @param knowledgeBaseId 知识库ID
     * @param keyword 检索关键词
     * @param scope 检索范围
     * @param folderId 文件夹ID
     * @param tagIds 标签ID列表
     * @param userId 用户ID
     */
    @GetMapping("/{knowledgeBaseId}/search")
    public Result<List<NoteSearchResultVO>> searchNotes(@PathVariable Long knowledgeBaseId,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false, defaultValue = "all") String scope,
                                                        @RequestParam(required = false) Long folderId,
                                                        @RequestParam(required = false) List<Long> tagIds,
                                                        @RequestAttribute("userId") Long userId) {
        return Result.success(
                knowledgeBaseService.searchNotes(userId, knowledgeBaseId, keyword, scope, folderId, tagIds)
        );
    }

    /**
     * 创建文件夹。
     * @param dto 创建文件夹请求参数
     * @param userId 用户ID
     */
    @PostMapping("/folders")
    public Result<TreeNodeVO> createFolder(@Valid @RequestBody CreateFolderDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("创建文件夹成功", knowledgeBaseService.createFolder(userId, dto));
    }

    /**
     * 更新文件夹。
     * @param folderId 文件夹ID
     * @param dto 更新文件夹请求参数
     * @param userId 用户ID
     */
    @PutMapping("/folders/{folderId}")
    public Result<TreeNodeVO> updateFolder(@PathVariable Long folderId,
                                           @Valid @RequestBody UpdateFolderDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("更新文件夹成功", knowledgeBaseService.updateFolder(userId, folderId, dto));
    }

    /**
     * 删除文件夹。
     * @param folderId 文件夹ID
     * @param userId 用户ID
     */
    @DeleteMapping("/folders/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long folderId,
                                     @RequestAttribute("userId") Long userId) {
        knowledgeBaseService.deleteFolder(userId, folderId);
        return Result.success("删除文件夹成功", null);
    }
}
