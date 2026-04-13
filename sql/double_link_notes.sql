/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80035 (8.0.35)
 Source Host           : localhost:3306
 Source Schema         : double_link_notes

 Target Server Type    : MySQL
 Target Server Version : 80035 (8.0.35)
 File Encoding         : 65001

 Date: 12/04/2026 21:08:44
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 文件夹同一父目录下重名由后端业务层校验，联合索引用于查询加速。
-- ----------------------------
-- Table structure for t_folder
-- ----------------------------
DROP TABLE IF EXISTS `t_folder`;
CREATE TABLE `t_folder`  (
  `id` bigint NOT NULL COMMENT '文件夹id',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库id',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父文件夹id，根目录为空',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件夹名称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1正常，2回收站）',
  `delete_token` bigint NOT NULL DEFAULT 0 COMMENT '软删除占位，0=活跃，删除后写本行id',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_t_folder_kb_parent_name_del`(`knowledge_base_id` ASC, `parent_id` ASC, `name` ASC, `delete_token` ASC) USING BTREE,
  INDEX `idx_t_folder_kb_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_t_folder_parent_id`(`parent_id` ASC) USING BTREE,
  CONSTRAINT `fk_t_folder_knowledge_base_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `t_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_t_folder_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `t_folder` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件夹表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `t_knowledge_base`;
CREATE TABLE `t_knowledge_base`  (
  `id` bigint NOT NULL COMMENT '知识库id',
  `user_id` bigint NOT NULL COMMENT '所属用户id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1正常，0删除/禁用）',
  `delete_token` bigint NOT NULL DEFAULT 0 COMMENT '软删除占位，0=活跃，删除后写本行id',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_knowledge_base_user_name_active`(`user_id` ASC, `name` ASC, `delete_token` ASC) USING BTREE,
  INDEX `idx_t_knowledge_base_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_t_knowledge_base_user_id` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_note
-- ----------------------------
DROP TABLE IF EXISTS `t_note`;
CREATE TABLE `t_note`  (
  `id` bigint NOT NULL COMMENT '笔记id',
  `user_id` bigint NOT NULL COMMENT '所属用户id',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库id',
  `folder_id` bigint NULL DEFAULT NULL COMMENT '所属文件夹id，可为空，表示知识库根目录',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '笔记标题',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '笔记状态，如(1:正常, 2:回收站)',
  `delete_token` bigint NOT NULL DEFAULT 0 COMMENT '软删除占位，0=活跃，删除后写本行id',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_note_kb_title_active`(`knowledge_base_id` ASC, `title` ASC, `delete_token` ASC) USING BTREE,
  INDEX `idx_t_note_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_t_note_kb_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_t_note_folder_id`(`folder_id` ASC) USING BTREE,
  INDEX `idx_t_note_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_t_note_folder_id` FOREIGN KEY (`folder_id`) REFERENCES `t_folder` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_t_note_knowledge_base_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `t_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_t_note_user_id` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_note_attachment
-- ----------------------------
DROP TABLE IF EXISTS `t_note_attachment`;
CREATE TABLE `t_note_attachment`  (
  `id` bigint NOT NULL COMMENT '附件id',
  `note_id` bigint NOT NULL COMMENT '所属笔记id',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名称',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型，如 image、video、file',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件访问路径',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MIME类型',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_t_note_attachment_note_id`(`note_id` ASC) USING BTREE,
  CONSTRAINT `fk_t_note_attachment_note_id` FOREIGN KEY (`note_id`) REFERENCES `t_note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记附件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_note_content
-- ----------------------------
DROP TABLE IF EXISTS `t_note_content`;
CREATE TABLE `t_note_content`  (
  `note_id` bigint NOT NULL COMMENT '笔记id',
  `markdown_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'Markdown原始内容',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`note_id`) USING BTREE,
  CONSTRAINT `fk_t_note_content_note_id` FOREIGN KEY (`note_id`) REFERENCES `t_note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记内容表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_note_history
-- ----------------------------
DROP TABLE IF EXISTS `t_note_history`;
CREATE TABLE `t_note_history`  (
  `id` bigint NOT NULL COMMENT '历史记录id',
  `note_id` bigint NOT NULL COMMENT '笔记id',
  `version_no` int NOT NULL COMMENT '版本号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当时版本标题',
  `markdown_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '当时版本Markdown内容',
  `created_by` bigint NOT NULL COMMENT '创建人id',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_note_history_note_version`(`note_id` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_t_note_history_created_by`(`created_by` ASC) USING BTREE,
  CONSTRAINT `fk_t_note_history_created_by` FOREIGN KEY (`created_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_t_note_history_note_id` FOREIGN KEY (`note_id`) REFERENCES `t_note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_note_link
-- ----------------------------
DROP TABLE IF EXISTS `t_note_link`;
CREATE TABLE `t_note_link`  (
  `id` bigint NOT NULL COMMENT '主键id',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库id',
  `source_note_id` bigint NOT NULL COMMENT '源笔记id',
  `target_note_id` bigint NULL DEFAULT NULL COMMENT '目标笔记id，可为空',
  `target_note_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始目标名称，如[[Redis]]中的Redis',
  `anchor_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '链接显示文本',
  `context_snippet` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '链接上下文摘要（用于展示）',
  `is_broken` tinyint NOT NULL DEFAULT 0 COMMENT '是否为失效链接（1是，0否）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_note_link_source_target_name`(`source_note_id` ASC, `target_note_name` ASC) USING BTREE,
  INDEX `idx_t_note_link_kb_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_t_note_link_target_note_id`(`target_note_id` ASC) USING BTREE,
  INDEX `idx_t_note_link_is_broken`(`is_broken` ASC) USING BTREE,
  CONSTRAINT `fk_t_note_link_knowledge_base_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `t_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_t_note_link_source_note_id` FOREIGN KEY (`source_note_id`) REFERENCES `t_note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_t_note_link_target_note_id` FOREIGN KEY (`target_note_id`) REFERENCES `t_note` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '双链关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_note_tag
-- ----------------------------
DROP TABLE IF EXISTS `t_note_tag`;
CREATE TABLE `t_note_tag`  (
  `id` bigint NOT NULL COMMENT '主键id',
  `note_id` bigint NOT NULL COMMENT '笔记id',
  `tag_id` bigint NOT NULL COMMENT '标签id',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_note_tag_note_tag`(`note_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_t_note_tag_note_id`(`note_id` ASC) USING BTREE,
  INDEX `idx_t_note_tag_tag_id`(`tag_id` ASC) USING BTREE,
  CONSTRAINT `fk_t_note_tag_note_id` FOREIGN KEY (`note_id`) REFERENCES `t_note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_t_note_tag_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `t_tag` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_note_template
-- ----------------------------
DROP TABLE IF EXISTS `t_note_template`;
CREATE TABLE `t_note_template`  (
  `id` bigint NOT NULL COMMENT '模板id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模板描述',
  `template_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '模板内容',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_note_template_user_name`(`user_id` ASC, `name` ASC) USING BTREE,
  INDEX `idx_t_note_template_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_t_note_template_user_id` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_tag
-- ----------------------------
DROP TABLE IF EXISTS `t_tag`;
CREATE TABLE `t_tag`  (
  `id` bigint NOT NULL COMMENT '标签id',
  `knowledge_base_id` bigint NOT NULL COMMENT '所属知识库id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_tag_kb_name`(`knowledge_base_id` ASC, `name` ASC) USING BTREE,
  INDEX `idx_t_tag_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE,
  CONSTRAINT `fk_t_tag_knowledge_base_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `t_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL COMMENT '用户id',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱（以后支持找回密码）',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_user_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_t_user_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user_settings
-- ----------------------------
DROP TABLE IF EXISTS `t_user_settings`;
CREATE TABLE `t_user_settings`  (
  `id` bigint NOT NULL COMMENT '主键id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `code_theme` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '代码主题',
  `content_theme` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容主题',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_user_settings_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_t_user_settings_user_id` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户设置表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
