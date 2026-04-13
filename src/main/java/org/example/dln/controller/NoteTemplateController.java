package org.example.dln.controller;

import jakarta.validation.Valid;
import org.example.dln.dto.CreateNoteTemplateDTO;
import org.example.dln.dto.UpdateNoteTemplateDTO;
import org.example.dln.service.NoteTemplateService;
import org.example.dln.vo.NoteTemplateVO;
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

import java.util.List;

/**
 * 包名：org.example.dln.controller
 * 类名：NoteTemplateController
 * 类描述：提供笔记模板管理相关接口。
 * 创建人：@author Rain_润
 */
@RestController
@RequestMapping("/templates")
public class NoteTemplateController {
    @Autowired
    private NoteTemplateService noteTemplateService;

    /**
    * 查询模板列表。
    */
    @GetMapping
    public Result<List<NoteTemplateVO>> listTemplates(@RequestAttribute("userId") Long userId) {
        return Result.success(noteTemplateService.listTemplates(userId));
    }

    /**
    * 创建模板。
    */
    @PostMapping
    public Result<NoteTemplateVO> createTemplate(@Valid @RequestBody CreateNoteTemplateDTO dto,
                                                 @RequestAttribute("userId") Long userId) {
        return Result.success("创建模板成功", noteTemplateService.createTemplate(userId, dto));
    }

    /**
    * 更新模板。
    */
    @PutMapping("/{templateId}")
    public Result<NoteTemplateVO> updateTemplate(@PathVariable Long templateId,
                                                 @Valid @RequestBody UpdateNoteTemplateDTO dto,
                                                 @RequestAttribute("userId") Long userId) {
        return Result.success("更新模板成功", noteTemplateService.updateTemplate(userId, templateId, dto));
    }

    /**
    * 删除模板。
    */
    @DeleteMapping("/{templateId}")
    public Result<Void> deleteTemplate(@PathVariable Long templateId,
                                       @RequestAttribute("userId") Long userId) {
        noteTemplateService.deleteTemplate(userId, templateId);
        return Result.success("删除模板成功", null);
    }
}
