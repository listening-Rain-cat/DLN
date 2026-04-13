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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
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
        user.setStatus(1);

        if (userMapper.insert(user) <= 0) {
            throw new BusinessException("注册失败，请稍后重试");
        }

        userSettingsService.ensureUserSettings(user.getId());
        return toUserInfoVO(user);
    }

    /**
    * 处理用户登录。
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
        BeanUtils.copyProperties(user, loginVO);
        loginVO.setToken(JwtUtil.generateToken(user.getId(), user.getUsername()));
        return loginVO;
    }

    /**
    * 上传用户头像。
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

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = getFileExtension(originalFileName);
        String relativePath = buildRelativePath(extension);
        Path absolutePath = buildAbsolutePath(relativePath);
        Path oldAvatarPath = resolveManagedAvatarPath(user.getAvatarUrl());

        try {
            Files.createDirectories(absolutePath.getParent());
            file.transferTo(absolutePath);
        } catch (IOException e) {
            throw new BusinessException("保存头像文件失败");
        }

        user.setAvatarUrl("/avatars/" + relativePath.replace("\\", "/"));

        try {
            if (userMapper.updateById(user) <= 0) {
                deletePhysicalFile(absolutePath);
                throw new BusinessException("更新头像失败，请稍后重试");
            }
        } catch (RuntimeException e) {
            deletePhysicalFile(absolutePath);
            throw e;
        }

        if (oldAvatarPath != null && !oldAvatarPath.equals(absolutePath)) {
            deletePhysicalFile(oldAvatarPath);
        }
        return toUserInfoVO(user);
    }

    /**
    * 更新用户信息。
    */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UpdateUserDTO updateUserDTO) {
        User user = userMapper.selectByUserId(userId);
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
    * 获取用户信息。
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
    */
    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        return userInfoVO;
    }

    /**
    * 判断字符串是否包含有效文本。
    */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
    * 清理文件名。
    */
    private String sanitizeFileName(String originalFileName) {
        String fileName = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName);
        if (!StringUtils.hasText(fileName)) {
            return "avatar";
        }
        return Path.of(fileName).getFileName().toString();
    }

    /**
    * 提取文件扩展名。
    */
    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index).toLowerCase();
    }

    /**
    * 构建相对路径。
    */
    private String buildRelativePath(String extension) {
        LocalDate today = LocalDate.now();
        return Paths.get(String.valueOf(today.getYear()),
                        String.format("%02d", today.getMonthValue()),
                        String.format("%02d", today.getDayOfMonth()),
                        UUID.randomUUID() + extension)
                .toString();
    }

    /**
    * 构建绝对路径。
    */
    private Path buildAbsolutePath(String relativePath) {
        return Paths.get(avatarUploadDir).toAbsolutePath().normalize().resolve(relativePath).normalize();
    }

    /**
    * 解析受管头像文件路径。
    */
    private Path resolveManagedAvatarPath(String avatarUrl) {
        if (!hasText(avatarUrl) || !avatarUrl.startsWith("/avatars/")) {
            return null;
        }
        String relativePath = avatarUrl.substring("/avatars/".length());
        return buildAbsolutePath(relativePath);
    }

    /**
    * 删除物理文件。
    */
    private void deletePhysicalFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
