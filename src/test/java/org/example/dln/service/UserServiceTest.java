package org.example.dln.service;

import org.example.dln.entity.User;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.UserMapper;
import org.example.dln.vo.UserInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 包名：org.example.dln.service
 * 类名：UserServiceTest
 * 类描述：测试用户服务相关业务逻辑，特别是头像上传功能。
 * 创建人：@author Rain_润
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars";

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 设置头像上传目录
        ReflectionTestUtils.setField(userService, "avatarUploadDir", AVATAR_UPLOAD_DIR);
    }

    @Test
    void uploadAvatarShouldRejectNullFile() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.uploadAvatar(USER_ID, null));
        assertEquals("上传的头像文件不能为空", exception.getMessage());
    }

    @Test
    void uploadAvatarShouldRejectEmptyFile() throws IOException {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[0]
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.uploadAvatar(USER_ID, emptyFile));
        assertEquals("上传的头像文件不能为空", exception.getMessage());
    }

    @Test
    void uploadAvatarShouldRejectOversizedFile() throws IOException {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                largeContent
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.uploadAvatar(USER_ID, largeFile));
        assertEquals("头像文件大小不能超过 5MB", exception.getMessage());
    }

    @Test
    void uploadAvatarShouldRejectUnsupportedType() throws IOException {
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file",
                "avatar.bmp",
                "image/bmp",
                new byte[]{1, 2, 3}
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.uploadAvatar(USER_ID, unsupportedFile));
        assertEquals("仅支持 jpg、png、gif、webp 格式的头像图片", exception.getMessage());
    }

    @Test
    void uploadAvatarShouldRejectNonExistentUser() throws IOException {
        MockMultipartFile file = createValidJpgFile();
        when(userMapper.selectByUserId(USER_ID)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.uploadAvatar(USER_ID, file));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void uploadAvatarShouldHandleJpegFormat() throws IOException {
        MockMultipartFile file = createValidJpgFile();
        User user = buildUser(USER_ID);
        when(userMapper.selectByUserId(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserInfoVO result = userService.uploadAvatar(USER_ID, file);

        assertNotNull(result);
        assertTrue(result.getAvatarUrl().startsWith("/avatars/"));
        assertTrue(result.getAvatarUrl().endsWith(".jpg"));
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void uploadAvatarShouldHandlePngFormat() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        User user = buildUser(USER_ID);
        when(userMapper.selectByUserId(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserInfoVO result = userService.uploadAvatar(USER_ID, file);

        assertNotNull(result);
        assertTrue(result.getAvatarUrl().endsWith(".png"));
    }

    @Test
    void uploadAvatarShouldHandleGifFormat() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/gif",
                new byte[]{1, 2, 3}
        );
        User user = buildUser(USER_ID);
        when(userMapper.selectByUserId(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserInfoVO result = userService.uploadAvatar(USER_ID, file);

        assertNotNull(result);
        assertTrue(result.getAvatarUrl().endsWith(".gif"));
    }

    @Test
    void uploadAvatarShouldHandleWebpFormat() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.webp",
                "image/webp",
                new byte[]{1, 2, 3}
        );
        User user = buildUser(USER_ID);
        when(userMapper.selectByUserId(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserInfoVO result = userService.uploadAvatar(USER_ID, file);

        assertNotNull(result);
        assertTrue(result.getAvatarUrl().endsWith(".webp"));
    }

    @Test
    void uploadAvatarShouldDeleteFileWhenUpdateFails() throws IOException {
        MockMultipartFile file = createValidJpgFile();
        User user = buildUser(USER_ID);
        when(userMapper.selectByUserId(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.uploadAvatar(USER_ID, file));
        assertEquals("更新头像失败，请稍后重试", exception.getMessage());
    }

    @Test
    void uploadAvatarShouldReturnUserInfoVOWithCorrectId() throws IOException {
        MockMultipartFile file = createValidJpgFile();
        User user = buildUser(USER_ID);
        when(userMapper.selectByUserId(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserInfoVO result = userService.uploadAvatar(USER_ID, file);

        assertNotNull(result);
        assertEquals(String.valueOf(USER_ID), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getNickname(), result.getNickname());
    }

    @Test
    void uploadAvatarShouldSetCorrectAvatarUrl() throws IOException {
        MockMultipartFile file = createValidJpgFile();
        User user = buildUser(USER_ID);
        when(userMapper.selectByUserId(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        userService.uploadAvatar(USER_ID, file);

        verify(userMapper).updateById(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertNotNull(updatedUser.getAvatarUrl());
        assertTrue(updatedUser.getAvatarUrl().startsWith("/avatars/"));
        assertTrue(updatedUser.getAvatarUrl().endsWith(".jpg"));
    }

    @Test
    void uploadAvatarShouldCreateDirectoriesIfNotExists() throws IOException {
        MockMultipartFile file = createValidJpgFile();
        User user = buildUser(USER_ID);
        when(userMapper.selectByUserId(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        userService.uploadAvatar(USER_ID, file);

        Path uploadDir = Paths.get(AVATAR_UPLOAD_DIR).toAbsolutePath().normalize();
        assertTrue(Files.exists(uploadDir));
    }

    private MockMultipartFile createValidJpgFile() {
        return new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
    }

    private User buildUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setNickname("Test User");
        user.setAvatarUrl(null);
        user.setStatus(1);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        return user;
    }
}
