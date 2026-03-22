package org.example.dln.service;

import org.example.dln.dto.LoginDTO;
import org.example.dln.dto.RegisterDTO;
import org.example.dln.dto.UpdateUserDTO;
import org.example.dln.entity.User;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.UserMapper;
import org.example.dln.util.JwtUtil;
import org.example.dln.vo.LoginVO;
import org.example.dln.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 包名：org.example.dln.service
 * 类名：UserService
 * 类描述：处理用户登录、注册、查询资料和修改资料等业务逻辑。
 * 创建人：@author Rain_润
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户注册。
     *
     * @param registerDTO 注册参数
     * @return 注册成功后的用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername().trim();
        String email = registerDTO.getEmail().trim();
        String nickname = registerDTO.getNickname().trim();

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(email);
        user.setNickname(nickname);
        user.setStatus(1);

        if (userMapper.insert(user) <= 0) {
            throw new BusinessException("注册失败，请稍后重试");
        }
        return toUserInfoVO(user);
    }

    /**
     * 用户登录校验。
     *
     * @param loginDTO 登录参数
     * @return 登录成功后的返回对象
     */
    public LoginVO login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername().trim();
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        LoginVO loginVO = new LoginVO();
        BeanUtils.copyProperties(user, loginVO);
        loginVO.setToken(JwtUtil.generateToken(user.getId(), user.getUsername()));
        return loginVO;
    }

    /**
     * 更新当前用户资料，可选修改密码。
     *
     * @param userId 当前用户 ID
     * @param updateUserDTO 更新参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UpdateUserDTO updateUserDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        boolean hasOldPassword = hasText(updateUserDTO.getOldPassword());
        boolean hasNewPassword = hasText(updateUserDTO.getNewPassword());
        if (hasOldPassword != hasNewPassword) {
            throw new BusinessException("修改密码时必须同时填写旧密码和新密码");
        }
        if (hasOldPassword) {
            String oldPassword = updateUserDTO.getOldPassword().trim();
            String newPassword = updateUserDTO.getNewPassword().trim();
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException("旧密码错误");
            }
            if (oldPassword.equals(newPassword)) {
                throw new BusinessException("新密码不能与旧密码相同");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        String email = updateUserDTO.getEmail().trim();
        User existingEmail = userMapper.findByEmail(email);
        if (existingEmail != null && !existingEmail.getId().equals(userId)) {
            throw new BusinessException("邮箱已被其他账号注册");
        }

        user.setNickname(updateUserDTO.getNickname().trim());
        user.setEmail(email);
        user.setAvatarUrl(hasText(updateUserDTO.getAvatarUrl()) ? updateUserDTO.getAvatarUrl().trim() : null);

        if (userMapper.updateById(user) <= 0) {
            throw new BusinessException("更新失败，请稍后重试");
        }
    }

    /**
     * 获取指定用户信息。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserInfoVO(user);
    }

    /**
     * 将用户实体转换为对外返回对象，避免暴露敏感字段。
     *
     * @param user 用户实体
     * @return 用户信息视图对象
     */
    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        return userInfoVO;
    }

    /**
     * 判断字符串是否包含有效文本。
     *
     * @param value 待判断内容
     * @return 是否为非空白字符串
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
