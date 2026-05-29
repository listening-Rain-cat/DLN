package org.example.dln.controller;

import org.example.dln.security.CurrentUserId;
import org.example.dln.entity.NoteAttachment;
import org.example.dln.exception.BusinessException;
import org.example.dln.service.AttachmentService;
import org.example.dln.vo.NoteAttachmentVO;
import org.example.dln.vo.Result;
import org.example.dln.vo.VditorUploadResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 包名：org.example.dln.controller
 * 类名：AttachmentController
 * 类描述：提供笔记附件与图片上传下载相关接口。
 * 创建人：@author Rain_润
 */
@RestController
@RequestMapping
public class AttachmentController {
    @Autowired
    private AttachmentService attachmentService;

    /**
    * 上传附件并保存记录。
     * @param noteId 笔记ID
     * @param file 上传文件
     * @param fileType 文件类型
     * @param userId 用户ID
    */
    @PostMapping(value = "/notes/{noteId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<NoteAttachmentVO> uploadAttachment(@PathVariable Long noteId,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "fileType", required = false) String fileType,
                                                     @CurrentUserId Long userId) {
        return Result.success("上传附件成功", attachmentService.uploadAttachment(userId, noteId, file, fileType));
    }

    /**
    * 上传 Vditor 图片资源。
     * @param noteId 笔记ID
     * @param files 上传文件列表
     * @param singleFile 单个上传文件
     * @param userId 用户ID
    */
    @PostMapping(value = "/notes/{noteId}/attachments/vditor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VditorUploadResponseVO uploadVditorImages(@PathVariable Long noteId,
                                                     @RequestParam(value = "file[]", required = false) MultipartFile[] files,
                                                     @RequestParam(value = "file", required = false) MultipartFile singleFile,
                                                     @CurrentUserId Long userId) {
        VditorUploadResponseVO response = new VditorUploadResponseVO();
        List<MultipartFile> uploadFiles = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    uploadFiles.add(file);
                }
            }
        }

        if (singleFile != null && !singleFile.isEmpty()) {
            uploadFiles.add(singleFile);
        }

        if (uploadFiles.isEmpty()) {
            response.setCode(1);
            response.setMsg("上传文件不能为空");
            return response;
        }

        String firstErrorMessage = "";

        for (MultipartFile file : uploadFiles) {
            String fileName = resolveFileName(file);

            try {
                NoteAttachmentVO attachment = attachmentService.uploadAttachment(userId, noteId, file, "image");
                response.getData()
                        .getSuccMap()
                        .put(fileName, buildPublicAttachmentUrl(attachment.getFileUrl()));
            } catch (BusinessException e) {
                if (firstErrorMessage.isBlank()) {
                    firstErrorMessage = e.getMessage();
                }
                response.getData().getErrFiles().add(fileName);
            } catch (RuntimeException e) {
                if (firstErrorMessage.isBlank()) {
                    firstErrorMessage = "图片上传失败，请稍后重试";
                }
                response.getData().getErrFiles().add(fileName);
            }
        }

        if (response.getData().getSuccMap().isEmpty()) {
            response.setCode(1);
            response.setMsg(firstErrorMessage.isBlank() ? "图片上传失败，请稍后重试" : firstErrorMessage);
        }

        return response;
    }

    /**
    * 查询笔记附件列表。
     * @param noteId 笔记ID
     * @param userId 用户ID
    */
    @GetMapping("/notes/{noteId}/attachments")
    public Result<List<NoteAttachmentVO>> listNoteAttachments(@PathVariable Long noteId,
                                                              @CurrentUserId Long userId) {
        return Result.success(attachmentService.listNoteAttachments(userId, noteId));
    }

    /**
    * 删除附件。
     * @param attachmentId 附件ID
     * @param userId 用户ID
    */
    @DeleteMapping("/attachments/{attachmentId}")
    public Result<Void> deleteAttachment(@PathVariable Long attachmentId,
                                         @CurrentUserId Long userId) {
        attachmentService.deleteAttachment(userId, attachmentId);
        return Result.success("删除附件成功", null);
    }

    /**
    * 下载附件。
     * @param attachmentId 附件ID
     * @param userId 用户ID
    */
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId,
                                                       @CurrentUserId Long userId) {
        NoteAttachment attachment = attachmentService.getAttachmentForDownload(userId, attachmentId);
        Resource resource = attachmentService.loadAsResource(attachment);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getMimeType() != null && !attachment.getMimeType().isBlank()) {
            mediaType = MediaType.parseMediaType(attachment.getMimeType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                                .build()
                .toString())
                .body(resource);
    }

    /**
    * 解析上传文件名。
     * @param file 上传文件
    */
    private String resolveFileName(MultipartFile file) {
        String originalFileName = file == null ? null : file.getOriginalFilename();
        if (!StringUtils.hasText(originalFileName)) {
            return "unnamed";
        }
        return originalFileName;
    }

    /**
    * 构建附件公开访问地址。
     * @param fileUrl 附件访问路径
    */
    private String buildPublicAttachmentUrl(String fileUrl) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(fileUrl.startsWith("/") ? fileUrl : "/" + fileUrl)
                .toUriString();
    }
}
