package org.example.dln.controller;

import org.example.dln.entity.NoteAttachment;
import org.example.dln.service.AttachmentService;
import org.example.dln.vo.NoteAttachmentVO;
import org.example.dln.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 包名：org.example.dln.controller
 * 类名：AttachmentController
 * 类描述：附件控制器。
 * 创建人：@author Rain_润
 */
@RestController
@RequestMapping
public class AttachmentController {
    @Autowired
    private AttachmentService attachmentService;

    @PostMapping(value = "/notes/{noteId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<NoteAttachmentVO> uploadAttachment(@PathVariable Long noteId,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "fileType", required = false) String fileType,
                                                     @RequestAttribute("userId") Long userId) {
        return Result.success("上传附件成功", attachmentService.uploadAttachment(userId, noteId, file, fileType));
    }

    @GetMapping("/notes/{noteId}/attachments")
    public Result<List<NoteAttachmentVO>> listNoteAttachments(@PathVariable Long noteId,
                                                              @RequestAttribute("userId") Long userId) {
        return Result.success(attachmentService.listNoteAttachments(userId, noteId));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public Result<Void> deleteAttachment(@PathVariable Long attachmentId,
                                         @RequestAttribute("userId") Long userId) {
        attachmentService.deleteAttachment(userId, attachmentId);
        return Result.success("删除附件成功", null);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId,
                                                       @RequestAttribute("userId") Long userId) {
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
}
