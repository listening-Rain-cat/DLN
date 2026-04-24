package org.example.dln.service;

import org.example.dln.dto.LoginDTO;
import org.example.dln.dto.RegisterDTO;
import org.example.dln.dto.UpdateUserDTO;
import org.example.dln.entity.User;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.UserMapper;
import org.example.dln.util.JwtUtil;
import org.example.dln.util.LongStringUtils;
import org.example.dln.vo.LoginVO;
import org.example.dln.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 包名：org.example.dln.service
 * 类名：UserService
 * 类描述：处理用户注册、登录与信息维护业务逻辑。
 * 创建人：@author Rain_润
 */
@Service
public class UserService {
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserSettingsService userSettingsService;

    @Value("${app.avatar.upload-dir:uploads/avatars}")
    private String avatarUploadDir;

    /**
    * 处理用户注册。
     * @param registerDTO 注册请求参数
    */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername().trim();
        String email = registerDTO.getEmail().trim();
        String nickname = registerDTO.getNickname().trim();

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.selectByEmail(email) != null) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(email);
        user.setNickname(nickname);
        //TODO 要不要管理员？
        user.setStatus(1);

        if (userMapper.insert(user) <= 0) {
            throw new BusinessException("注册失败，请稍后重试");
        }
        userSettingsService.ensureUserSettings(user.getId());
        return toUserInfoVO(user);
    }

    /**
    * 处理用户登录。
     * @param loginDTO 登录请求参数
    */
    public LoginVO login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername().trim();
        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        LoginVO loginVO = new LoginVO();
        //所有实体类拷贝到VO视图对象中，long因前端数字精度问题，所有数字全部单独处理
        BeanUtils.copyProperties(user, loginVO);
        loginVO.setId(LongStringUtils.toStringValue(user.getId()));
        loginVO.setToken(JwtUtil.generateToken(user.getId(), user.getUsername()));
        return loginVO;
    }
    /**
     * 获取用户信息。
     * @param userId 用户ID
     */
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserInfoVO(user);
    }

    /**
     * 将用户实体转换为用户信息视图对象。
     * @param user 用户实体
     */
    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        userInfoVO.setId(LongStringUtils.toStringValue(user.getId()));
        return userInfoVO;
    }


    /**
    * 上传用户头像。
     * @param userId 用户ID
     * @param file 头像文件
    */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传的头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像文件大小不能超过 5MB");
        }
        if (!ALLOWED_AVATAR_TYPES.contains(file.getContentType())) {
            throw new BusinessException("仅支持 jpg、png、gif、webp 格式的头像图片");
        }

        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw new BusinessException("仅支持 jpg、png、gif、webp 格式的头像图片");
        };
        String fileName = UUID.randomUUID() + extension;
        Path absolutePath = Paths.get(avatarUploadDir)
                .toAbsolutePath()
                .normalize()
                .resolve(fileName)
                .normalize();

        try {
            Files.createDirectories(absolutePath.getParent());
            //保存文件到磁盘
            file.transferTo(absolutePath);
        } catch (IOException e) {
            throw new BusinessException("保存头像文件失败");
        }

        user.setAvatarUrl("/avatars/" + fileName);

        try {
            if (userMapper.updateById(user) <= 0) {
                try {
                    Files.deleteIfExists(absolutePath);
                } catch (IOException ignored) {
                }
                throw new BusinessException("更新头像失败，请稍后重试");
            }
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(absolutePath);
            } catch (IOException ignored) {
            }
            throw e;
        }
        return toUserInfoVO(user);
    }

    /**
    * 更新用户信息。
     * @param userId 用户ID
     * @param updateUserDTO 用户信息更新参数
    */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UpdateUserDTO updateUserDTO) {
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        boolean hasOldPassword = hasText(updateUserDTO.getOldPassword());
        boolean hasNewPassword = hasText(updateUserDTO.getNewPassword());
        //TODO 做邮箱认证后再改密码
        if (hasOldPassword != hasNewPassword) {
            throw new BusinessException("修改密码时必须同时填写旧密码和新密码");
        }
        if (hasOldPassword) {
            String oldPassword = updateUserDTO.getOldPassword().trim();
            String newPassword = updateUserDTO.getNewPassword().trim();
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException("当前密码错误");
            }
            if (oldPassword.equals(newPassword)) {
                throw new BusinessException("新密码不能与旧密码相同");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        String email = updateUserDTO.getEmail().trim();
        User existingEmail = userMapper.selectByEmail(email);
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
    * 判断字符串是否包含有效文本。
     * @param value 待处理的值
    */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
