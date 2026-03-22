package org.example.dln.controller;

import org.example.dln.dto.LoginDTO;
import org.example.dln.dto.RegisterDTO;
import org.example.dln.dto.UpdateUserDTO;
import org.example.dln.service.UserService;
import org.example.dln.vo.LoginVO;
import org.example.dln.vo.Result;
import org.example.dln.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 包名：org.example.dln.controller
 * 类名：UserController
 * 类描述：用户相关接口控制器，提供登录、注册、查询个人信息和修改个人信息功能。
 * 创建人：@author Rain_润
 */
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 用户注册。
     *
     * @param registerDTO 注册参数
     * @return 注册成功后的用户信息
     */
    @PostMapping("/register")
    public Result<UserInfoVO> register(@Validated @RequestBody RegisterDTO registerDTO) {
        UserInfoVO userInfoVO = userService.register(registerDTO);
        return Result.success("注册成功", userInfoVO);
    }

    /**
     * 用户登录。
     *
     * @param loginDTO 登录参数
     * @return 登录结果，包含用户信息和 token
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success("登录成功", loginVO);
    }

    /**
     * 获取当前登录用户的个人信息。
     *
     * @param userId 当前登录用户 ID
     * @return 当前用户信息
     */
    @GetMapping("/userInfo")
    public Result<UserInfoVO> getUserInfo(@RequestAttribute("userId") Long userId) {
        UserInfoVO userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    /**
     * 修改当前登录用户的个人信息。
     *
     * @param updateUserDTO 更新参数
     * @param userId 当前登录用户 ID
     * @return 更新结果
     */
    @PutMapping("/user")
    public Result<Void> updateUser(@Validated @RequestBody UpdateUserDTO updateUserDTO,
                                   @RequestAttribute("userId") Long userId) {
        userService.updateUser(userId, updateUserDTO);
        return Result.success("用户信息更新成功", null);
    }
}
