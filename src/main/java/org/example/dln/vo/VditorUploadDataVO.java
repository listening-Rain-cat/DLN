package org.example.dln.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 包名：org.example.dln.vo
 * 类名：VditorUploadDataVO
 * 类描述：定义 Vditor 上传结果数据展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class VditorUploadDataVO {
    private List<String> errFiles = new ArrayList<>();
    private Map<String, String> succMap = new LinkedHashMap<>();
}
