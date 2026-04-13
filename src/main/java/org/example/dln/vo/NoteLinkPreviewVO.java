package org.example.dln.vo;

import lombok.Data;

/**
 * 包名：org.example.dln.vo
 * 类名：NoteLinkPreviewVO
 * 类描述：定义笔记链接预览展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class NoteLinkPreviewVO {
    private Long noteId;
    private String title;
    private String markdownContent;
    private Integer isBroken;
}
