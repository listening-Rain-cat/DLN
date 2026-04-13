package org.example.dln.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 包名：org.example.dln.dto
 * 类名：UpdateFolderDTO
 * 类描述：用于承载更新文件夹请求参数。
 * 创建人：@author Rain_润
 */
@Data
public class UpdateFolderDTO {
    private Long parentId;

    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 100, message = "文件夹名称不能超过 100 个字符")
    private String name;
}
