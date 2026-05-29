package org.example.dln.controller;

import org.example.dln.security.CurrentUserId;
import org.example.dln.dto.LoginDTO;
import org.example.dln.dto.RegisterDTO;
import org.example.dln.dto.UpdateUserDTO;
import org.example.dln.dto.UpdateUserSettingsDTO;
import org.example.dln.service.UserService;
import org.example.dln.service.UserSettingsService;
import org.example.dln.vo.LoginVO;
import org.example.dln.vo.Result;
import org.example.dln.vo.UserInfoVO;
import org.example.dln.vo.UserSettingsVO;
import org.example.dln.vo.VditorThemeOptionsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 包名：org.example.dln.controller
 * 类名：UserController
 * 类描述：提供用户注册、登录、信息维护和设置相关接口。
 * 创建人：@author Rain_润
 */
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserSettingsService userSettingsService;

    /**
    * 处理用户注册。
     * @param registerDTO 注册请求参数
    */
    @PostMapping("/register")
    public Result<UserInfoVO> register(@Validated @RequestBody RegisterDTO registerDTO) {
        UserInfoVO userInfoVO = userService.register(registerDTO);
        return Result.success("注册成功", userInfoVO);
    }

    /**
    * 处理用户登录。
     * @param loginDTO 登录请求参数
    */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success("登录成功", loginVO);
    }

    /**
    * 获取用户信息。
     * @param userId 用户ID
    */
    @GetMapping("/userInfo")
    public Result<UserInfoVO> getUserInfo(@CurrentUserId Long userId) {
        UserInfoVO userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    /**
    * 获取用户设置。
     * @param userId 用户ID
    */
    @GetMapping("/user/settings")
    public Result<UserSettingsVO> getUserSettings(@CurrentUserId Long userId) {
        UserSettingsVO userSettingsVO = userSettingsService.getUserSettings(userId);
        return Result.success(userSettingsVO);
    }

    /**
    * 获取主题选项。
    */
    @GetMapping("/user/settings/theme-options")
    public Result<VditorThemeOptionsVO> getThemeOptions() {
        VditorThemeOptionsVO themeOptionsVO = userSettingsService.getThemeOptions();
        return Result.success(themeOptionsVO);
    }

    /**
    * 更新用户信息。
     * @param updateUserDTO 用户信息更新参数
     * @param userId 用户ID
    */
    @PutMapping("/user")
    public Result<Void> updateUser(@Validated @RequestBody UpdateUserDTO updateUserDTO,
                                   @CurrentUserId Long userId) {
        userService.updateUser(userId, updateUserDTO);
        return Result.success("用户信息更新成功", null);
    }

    /**
    * 更新用户设置。
     * @param updateUserSettingsDTO 用户设置更新参数
     * @param userId 用户ID
    */
    @PutMapping("/user/settings")
    public Result<UserSettingsVO> updateUserSettings(@Validated @RequestBody UpdateUserSettingsDTO updateUserSettingsDTO,
                                                     @CurrentUserId Long userId) {
        UserSettingsVO userSettingsVO = userSettingsService.updateUserSettings(userId, updateUserSettingsDTO);
        return Result.success("用户设置更新成功", userSettingsVO);
    }

    /**
    * 上传用户头像。
     * @param file 头像文件
     * @param userId 用户ID
    */
    @PostMapping("/user/avatar")
    public Result<UserInfoVO> uploadAvatar(@RequestParam(value = "file") MultipartFile file,
                                           @CurrentUserId Long userId) {
        UserInfoVO userInfoVO = userService.uploadAvatar(userId, file);
        return Result.success("头像上传成功", userInfoVO);
    }
}
