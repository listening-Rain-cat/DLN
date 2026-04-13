package org.example.dln.vo;

import lombok.Data;

import java.util.Set;

/**
 * 包名：org.example.dln.vo
 * 类名：VditorThemeOptionsVO
 * 类描述：定义 Vditor 主题选项展示视图对象。
 * 创建人：@author Rain_润
 */
@Data
public class VditorThemeOptionsVO {
    private Set<String> contentThemes;
    private Set<String> codeThemes;
    private String defaultContentTheme;
    private String defaultCodeTheme;
}
