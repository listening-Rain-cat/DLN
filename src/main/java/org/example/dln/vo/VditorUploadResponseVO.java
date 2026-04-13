package org.example.dln.vo;

import lombok.Data;

/**
 * 包名：org.example.dln.vo
 * 类名：VditorUploadResponseVO
 * 类描述：定义 Vditor 上传响应展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class VditorUploadResponseVO {
    private String msg = "";
    private Integer code = 0;
    private VditorUploadDataVO data = new VditorUploadDataVO();
}
