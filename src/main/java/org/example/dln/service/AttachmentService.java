package org.example.dln.service;

import org.example.dln.entity.Note;
import org.example.dln.entity.NoteAttachment;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.NoteAttachmentMapper;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.util.LongStringUtils;
import org.example.dln.vo.NoteAttachmentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 包名：org.example.dln.service
 * 类名：AttachmentService
 * 类描述：处理笔记附件上传、查询、删除和资源访问逻辑。
 * 创建人：@author Rain_润
 */
@Service
public class AttachmentService {
    @Autowired
    private NoteAttachmentMapper noteAttachmentMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Value("${app.attachment.upload-dir:uploads/attachments}")
    private String uploadDir;

    /**
    * 上传附件并保存记录。
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param file 上传文件
     * @param fileType 文件类型
    */
    @Transactional(rollbackFor = Exception.class)
    public NoteAttachmentVO uploadAttachment(Long userId, Long noteId, MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        Note note = getNoteOrThrow(userId, noteId);
        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = getFileExtension(originalFileName);
        String relativePath = buildRelativePath(extension);
        Path absolutePath = buildAbsolutePath(relativePath);

        try {
            Files.createDirectories(absolutePath.getParent());
            file.transferTo(absolutePath);
        } catch (IOException e) {
            throw new BusinessException("保存附件文件失败");
        }

        NoteAttachment attachment = new NoteAttachment();
        attachment.setNoteId(note.getId());
        attachment.setFileName(originalFileName);
        attachment.setFileType(resolveFileType(fileType, file.getContentType(), originalFileName));
        attachment.setFileUrl(relativePath.replace("\\", "/"));
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());

        try {
            if (noteAttachmentMapper.insert(attachment) <= 0) {
                deletePhysicalFile(absolutePath);
                throw new BusinessException("保存附件记录失败");
            }
        } catch (RuntimeException e) {
            deletePhysicalFile(absolutePath);
            throw e;
        }

        return toAttachmentVO(attachment);
    }

    /**
    * 查询笔记附件列表。
     * @param userId 用户ID
     * @param noteId 笔记ID
    */
    public List<NoteAttachmentVO> listNoteAttachments(Long userId, Long noteId) {
        getNoteOrThrow(userId, noteId);
        List<NoteAttachment> attachments = noteAttachmentMapper.selectByNoteIdOrderByCreatedTimeDesc(noteId);
        return buildAttachmentVOList(attachments);
    }

    /**
    * 删除附件。
     * @param userId 用户ID
     * @param attachmentId 附件ID
    */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long userId, Long attachmentId) {
        NoteAttachment attachment = getAttachmentOrThrow(userId, attachmentId);
        Path absolutePath = buildAbsolutePath(attachment.getFileUrl());
        if (noteAttachmentMapper.deleteById(attachmentId) <= 0) {
            throw new BusinessException("删除附件失败");
        }
        deletePhysicalFile(absolutePath);
    }

    /**
    * 获取用于下载的附件实体。
     * @param userId 用户ID
     * @param attachmentId 附件ID
    */
    public NoteAttachment getAttachmentForDownload(Long userId, Long attachmentId) {
        return getAttachmentOrThrow(userId, attachmentId);
    }

    /**
    * 将附件加载为资源对象。
     * @param attachment 附件实体
    */
    public Resource loadAsResource(NoteAttachment attachment) {
        Path absolutePath = buildAbsolutePath(attachment.getFileUrl());
        Resource resource = new FileSystemResource(absolutePath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException("附件文件不存在");
        }
        return resource;
    }

    /**
    * 获取附件，不存在时抛出异常。
     * @param userId 用户ID
     * @param attachmentId 附件ID
    */
    private NoteAttachment getAttachmentOrThrow(Long userId, Long attachmentId) {
        NoteAttachment attachment = noteAttachmentMapper.selectByAttachmentId(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        getNoteOrThrow(userId, attachment.getNoteId());
        return attachment;
    }

    /**
    * 获取笔记，不存在时抛出异常。
     * @param userId 用户ID
     * @param noteId 笔记ID
    */
    private Note getNoteOrThrow(Long userId, Long noteId) {
        Note note = noteMapper.selectByNoteId(noteId);
        if (note == null
                || !Objects.equals(note.getUserId(), userId)
                || note.getStatus() == null
                || note.getStatus() != 1
                || !Objects.equals(note.getDeleteToken(), 0L)) {
            throw new BusinessException("笔记不存在或无权限访问");
        }
        knowledgeBaseService.getKnowledgeBaseOrThrow(userId, note.getKnowledgeBaseId());
        return note;
    }

    /**
    * 清理文件名。
     * @param originalFileName 原始文件名
    */
    private String sanitizeFileName(String originalFileName) {
        String fileName = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName);
        if (!StringUtils.hasText(fileName)) {
            return "unnamed";
        }
        return Path.of(fileName).getFileName().toString();
    }

    /**
    * 提取文件扩展名。
     * @param fileName 文件名
    */
    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index);
    }

    /**
    * 构建相对路径。
     * @param extension 文件扩展名
    */
    private String buildRelativePath(String extension) {
        LocalDate today = LocalDate.now();
        return Paths.get(String.valueOf(today.getYear()),
                        String.format("%02d", today.getMonthValue()),
                        String.format("%02d", today.getDayOfMonth()),
                        UUID.randomUUID() + extension)
                .toString();
    }

    /**
    * 构建绝对路径。
     * @param relativePath 相对路径
    */
    private Path buildAbsolutePath(String relativePath) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(relativePath).normalize();
    }

    /**
    * 解析文件类型。
     * @param fileType 文件类型
     * @param mimeType 文件MIME类型
     * @param fileName 文件名
    */
    private String resolveFileType(String fileType, String mimeType, String fileName) {
        if (StringUtils.hasText(fileType)) {
            return fileType.trim();
        }
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return "image";
            }
            if (mimeType.startsWith("video/")) {
                return "video";
            }
            if (mimeType.startsWith("audio/")) {
                return "audio";
            }
        }
        String extension = getFileExtension(fileName).toLowerCase();
        if (".md".equals(extension) || ".txt".equals(extension) || ".pdf".equals(extension)) {
            return "file";
        }
        return "file";
    }

    /**
    * 删除物理文件。
     * @param path 文件路径
    */
    private void deletePhysicalFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    /**
    * 构建附件视图对象列表。
     * @param attachments 附件列表
    */
    private List<NoteAttachmentVO> buildAttachmentVOList(List<NoteAttachment> attachments) {
        List<NoteAttachmentVO> result = new ArrayList<>();
        for (NoteAttachment attachment : attachments) {
            result.add(toAttachmentVO(attachment));
        }
        return result;
    }

    /**
    * 将附件实体转换为附件视图对象。
     * @param attachment 附件实体
    */
    private NoteAttachmentVO toAttachmentVO(NoteAttachment attachment) {
        NoteAttachmentVO vo = new NoteAttachmentVO();
        BeanUtils.copyProperties(attachment, vo);
        vo.setId(LongStringUtils.toStringValue(attachment.getId()));
        vo.setNoteId(LongStringUtils.toStringValue(attachment.getNoteId()));
        vo.setFileSize(LongStringUtils.toStringValue(attachment.getFileSize()));
        vo.setFileUrl("/attachments/" + attachment.getId() + "/download");
        return vo;
    }
}
