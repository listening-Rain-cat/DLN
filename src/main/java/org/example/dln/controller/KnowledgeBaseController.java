package org.example.dln.controller;

import jakarta.validation.Valid;
import org.example.dln.dto.CreateFolderDTO;
import org.example.dln.dto.CreateKnowledgeBaseDTO;
import org.example.dln.dto.UpdateFolderDTO;
import org.example.dln.dto.UpdateKnowledgeBaseDTO;
import org.example.dln.service.KnowledgeBaseService;
import org.example.dln.vo.KnowledgeBaseVO;
import org.example.dln.vo.KnowledgeGraphVO;
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
     */
    @PostMapping
    public Result<KnowledgeBaseVO> createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseDTO dto,
                                                       @RequestAttribute("userId") Long userId) {
        return Result.success("创建知识库成功", knowledgeBaseService.createKnowledgeBase(userId, dto));
    }

    /**
     * 查询知识库列表。
     */
    @GetMapping
    public Result<List<KnowledgeBaseVO>> listKnowledgeBases(@RequestAttribute("userId") Long userId) {
        return Result.success(knowledgeBaseService.listKnowledgeBases(userId));
    }

    /**
     * 更新知识库。
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
     */
    @DeleteMapping("/{knowledgeBaseId}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long knowledgeBaseId,
                                            @RequestAttribute("userId") Long userId) {
        knowledgeBaseService.deleteKnowledgeBase(userId, knowledgeBaseId);
        return Result.success("删除知识库成功", null);
    }

    /**
     * 获取知识库目录树。
     */
    @GetMapping("/{knowledgeBaseId}/tree")
    public Result<List<TreeNodeVO>> getKnowledgeBaseTree(@PathVariable Long knowledgeBaseId,
                                                         @RequestAttribute("userId") Long userId) {
        return Result.success(knowledgeBaseService.getKnowledgeBaseTree(userId, knowledgeBaseId));
    }

    /**
     * 获取知识图谱。
     */
    @GetMapping("/{knowledgeBaseId}/graph")
    public Result<KnowledgeGraphVO> getKnowledgeGraph(@PathVariable Long knowledgeBaseId,
                                                      @RequestAttribute("userId") Long userId) {
        return Result.success(knowledgeBaseService.getKnowledgeGraph(userId, knowledgeBaseId));
    }

    /**
     * 创建文件夹。
     */
    @PostMapping("/folders")
    public Result<TreeNodeVO> createFolder(@Valid @RequestBody CreateFolderDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("创建文件夹成功", knowledgeBaseService.createFolder(userId, dto));
    }

    /**
     * 更新文件夹。
     */
    @PutMapping("/folders/{folderId}")
    public Result<TreeNodeVO> updateFolder(@PathVariable Long folderId,
                                           @Valid @RequestBody UpdateFolderDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success("更新文件夹成功", knowledgeBaseService.updateFolder(userId, folderId, dto));
    }

    /**
     * 删除文件夹。
     */
    @DeleteMapping("/folders/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long folderId,
                                     @RequestAttribute("userId") Long userId) {
        knowledgeBaseService.deleteFolder(userId, folderId);
        return Result.success("删除文件夹成功", null);
    }
}
