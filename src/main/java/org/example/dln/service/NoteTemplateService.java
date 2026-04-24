package org.example.dln.service;

import org.example.dln.dto.CreateNoteTemplateDTO;
import org.example.dln.dto.UpdateNoteTemplateDTO;
import org.example.dln.entity.NoteTemplate;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteTemplateMapper;
import org.example.dln.util.LongStringUtils;
import org.example.dln.vo.NoteTemplateVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 包名：org.example.dln.service
 * 类名：NoteTemplateService
 * 类描述：处理笔记模板管理相关业务逻辑。
 * 创建人：@author Rain_润
 */
@Service
public class NoteTemplateService {
    @Autowired
    private NoteTemplateMapper noteTemplateMapper;

    /**
    * 查询模板列表。
     * @param userId 用户ID
    */
    public List<NoteTemplateVO> listTemplates(Long userId) {
        return noteTemplateMapper.selectByUserIdOrderByUpdatedTimeDesc(userId)
                .stream()
                .map(this::toNoteTemplateVO)
                .toList();
    }

    /**
    * 创建模板。
     * @param userId 用户ID
     * @param dto 创建模板请求参数
    */
    @Transactional(rollbackFor = Exception.class)
    public NoteTemplateVO createTemplate(Long userId, CreateNoteTemplateDTO dto) {
        String name = dto.getName().trim();
        checkTemplateNameExists(userId, name, null);

        NoteTemplate template = new NoteTemplate();
        template.setUserId(userId);
        template.setName(name);
        template.setDescription(trimToNull(dto.getDescription()));
        template.setTemplateContent(dto.getTemplateContent());
        if (noteTemplateMapper.insert(template) <= 0) {
            throw new BusinessException("创建模板失败");
        }
        return toNoteTemplateVO(template);
    }

    /**
    * 更新模板。
     * @param userId 用户ID
     * @param templateId 模板ID
     * @param dto 更新模板请求参数
    */
    @Transactional(rollbackFor = Exception.class)
    public NoteTemplateVO updateTemplate(Long userId, Long templateId, UpdateNoteTemplateDTO dto) {
        NoteTemplate template = getTemplateOrThrow(userId, templateId);
        String name = dto.getName().trim();
        checkTemplateNameExists(userId, name, templateId);

        template.setName(name);
        template.setDescription(trimToNull(dto.getDescription()));
        template.setTemplateContent(dto.getTemplateContent());
        if (noteTemplateMapper.updateById(template) <= 0) {
            throw new BusinessException("更新模板失败");
        }
        return toNoteTemplateVO(template);
    }

    /**
    * 删除模板。
     * @param userId 用户ID
     * @param templateId 模板ID
    */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long userId, Long templateId) {
        NoteTemplate template = getTemplateOrThrow(userId, templateId);
        if (noteTemplateMapper.deleteById(template.getId()) <= 0) {
            throw new BusinessException("删除模板失败");
        }
    }

    /**
    * 获取模板，不存在时抛出异常。
     * @param userId 用户ID
     * @param templateId 模板ID
    */
    public NoteTemplate getTemplateOrThrow(Long userId, Long templateId) {
        NoteTemplate template = noteTemplateMapper.selectByTemplateIdAndUserId(templateId, userId);
        if (template == null) {
            throw new BusinessException("模板不存在或无权限访问");
        }
        return template;
    }

    /**
    * 检查模板名称是否已存在。
     * @param userId 用户ID
     * @param name 名称
     * @param excludeId 排除的模板ID（更新时使用，新增时传null）
    */
    private void checkTemplateNameExists(Long userId, String name, Long excludeId) {
        List<NoteTemplate> templates = noteTemplateMapper.selectByUserIdAndName(userId, name);
        boolean exists = templates.stream().anyMatch(template -> !Objects.equals(template.getId(), excludeId));
        if (exists) {
            throw new BusinessException("当前用户下已存在同名模板");
        }
    }

    /**
    * 将模板实体转换为模板视图对象。
     * @param template template参数
    */
    private NoteTemplateVO toNoteTemplateVO(NoteTemplate template) {
        NoteTemplateVO vo = new NoteTemplateVO();
        BeanUtils.copyProperties(template, vo);
        vo.setId(LongStringUtils.toStringValue(template.getId()));
        return vo;
    }

    /**
    * 去除字符串首尾空白并在为空时返回 null。
     * @param value 待处理的值
    */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
