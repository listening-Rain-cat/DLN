package org.example.dln.service;

import org.example.dln.dto.UpdateUserSettingsDTO;
import org.example.dln.entity.User;
import org.example.dln.entity.UserSettings;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.UserMapper;
import org.example.dln.mapper.UserSettingsMapper;
import org.example.dln.util.VditorThemeCatalog;
import org.example.dln.vo.VditorThemeOptionsVO;
import org.example.dln.vo.UserSettingsVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;

/**
 * 包名：org.example.dln.service
 * 类名：UserSettingsService
 * 类描述：处理用户个性化设置相关业务逻辑。
 * 创建人：@author Rain_润
 */
@Service
public class UserSettingsService {
    public static final String DEFAULT_CODE_THEME = "github";
    public static final String DEFAULT_CONTENT_THEME = "light";

    @Autowired
    private UserSettingsMapper userSettingsMapper;

    @Autowired
    private UserMapper userMapper;

    /**
    * 获取用户设置。
    */
    public UserSettingsVO getUserSettings(Long userId) {
        return toUserSettingsVO(ensureUserSettings(userId));
    }

    /**
    * 获取主题选项。
    */
    public VditorThemeOptionsVO getThemeOptions() {
        VditorThemeOptionsVO themeOptionsVO = new VditorThemeOptionsVO();
        themeOptionsVO.setContentThemes(new LinkedHashSet<>(VditorThemeCatalog.getContentThemes()));
        themeOptionsVO.setCodeThemes(new LinkedHashSet<>(VditorThemeCatalog.getCodeThemes()));
        themeOptionsVO.setDefaultContentTheme(DEFAULT_CONTENT_THEME);
        themeOptionsVO.setDefaultCodeTheme(DEFAULT_CODE_THEME);
        return themeOptionsVO;
    }

    /**
    * 更新用户设置。
    */
    @Transactional(rollbackFor = Exception.class)
    public UserSettingsVO updateUserSettings(Long userId, UpdateUserSettingsDTO updateUserSettingsDTO) {
        UserSettings userSettings = ensureUserSettings(userId);
        userSettings.setCodeTheme(validateCodeTheme(updateUserSettingsDTO.getCodeTheme()));
        userSettings.setContentTheme(validateContentTheme(updateUserSettingsDTO.getContentTheme()));

        if (userSettingsMapper.updateById(userSettings) <= 0) {
            throw new BusinessException("更新用户设置失败，请稍后重试");
        }

        return toUserSettingsVO(userSettings);
    }

    /**
    * 确保用户设置存在。
    */
    @Transactional(rollbackFor = Exception.class)
    public UserSettings ensureUserSettings(Long userId) {
        UserSettings userSettings = userSettingsMapper.selectByUserId(userId);
        if (userSettings != null) {
            String normalizedCodeTheme = normalizePersistedCodeTheme(userSettings.getCodeTheme());
            String normalizedContentTheme = normalizePersistedContentTheme(userSettings.getContentTheme());

            if (!normalizedCodeTheme.equals(userSettings.getCodeTheme())
                    || !normalizedContentTheme.equals(userSettings.getContentTheme())) {
                userSettings.setCodeTheme(normalizedCodeTheme);
                userSettings.setContentTheme(normalizedContentTheme);
                userSettingsMapper.updateById(userSettings);
            }
            return userSettings;
        }

        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserSettings created = new UserSettings();
        created.setUserId(userId);
        created.setCodeTheme(DEFAULT_CODE_THEME);
        created.setContentTheme(DEFAULT_CONTENT_THEME);

        if (userSettingsMapper.insert(created) <= 0) {
            throw new BusinessException("初始化用户设置失败，请稍后重试");
        }

        return created;
    }

    /**
    * 校验代码主题。
    */
    private String validateCodeTheme(String value) {
        String theme = normalizeThemeName(value, DEFAULT_CODE_THEME);
        if (!VditorThemeCatalog.isAllowedCodeTheme(theme)) {
            throw new BusinessException("不支持的代码主题：" + theme);
        }
        return theme;
    }

    /**
    * 校验内容主题。
    */
    private String validateContentTheme(String value) {
        String theme = normalizeThemeName(value, DEFAULT_CONTENT_THEME);
        if (!VditorThemeCatalog.isAllowedContentTheme(theme)) {
            throw new BusinessException("不支持的内容主题：" + theme);
        }
        return theme;
    }

    /**
    * 规范化持久化的代码主题值。
    */
    private String normalizePersistedCodeTheme(String value) {
        String theme = normalizeThemeName(value, DEFAULT_CODE_THEME);
        return VditorThemeCatalog.isAllowedCodeTheme(theme) ? theme : DEFAULT_CODE_THEME;
    }

    /**
    * 规范化持久化的内容主题值。
    */
    private String normalizePersistedContentTheme(String value) {
        String theme = normalizeThemeName(value, DEFAULT_CONTENT_THEME);
        return VditorThemeCatalog.isAllowedContentTheme(theme) ? theme : DEFAULT_CONTENT_THEME;
    }

    /**
    * 规范化主题名称。
    */
    private String normalizeThemeName(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /**
    * 将用户设置实体转换为用户设置视图对象。
    */
    private UserSettingsVO toUserSettingsVO(UserSettings userSettings) {
        UserSettingsVO userSettingsVO = new UserSettingsVO();
        BeanUtils.copyProperties(userSettings, userSettingsVO);
        userSettingsVO.setCodeTheme(normalizePersistedCodeTheme(userSettings.getCodeTheme()));
        userSettingsVO.setContentTheme(normalizePersistedContentTheme(userSettings.getContentTheme()));
        return userSettingsVO;
    }
}
