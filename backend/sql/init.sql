-- =====================================================
-- 智屏管理系统 数据库初始化脚本
-- Database: screen_cast_hub
-- Version: 1.0.0
-- Date: 2026-03-17
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `screen_cast_hub` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `screen_cast_hub`;

-- =====================================================
-- 用户表
-- =====================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(50) NOT NULL COMMENT '用户名',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `password` varchar(100) NOT NULL COMMENT '密码',
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
    `status` tinyint DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    `last_login_at` datetime DEFAULT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 设备分组表
-- =====================================================
CREATE TABLE IF NOT EXISTS `device_group` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `name` varchar(100) NOT NULL COMMENT '分组名称',
    `description` varchar(500) DEFAULT NULL COMMENT '分组描述',
    `sort_order` int DEFAULT 0 COMMENT '排序',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分组表';

-- =====================================================
-- 设备表
-- =====================================================
CREATE TABLE IF NOT EXISTS `device` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint DEFAULT NULL COMMENT '绑定用户ID',
    `group_id` bigint DEFAULT NULL COMMENT '设备分组ID',
    `device_token` varchar(64) NOT NULL COMMENT '设备唯一标识',
    `bind_code` varchar(32) DEFAULT NULL COMMENT '绑定码',
    `bind_code_expire` datetime DEFAULT NULL COMMENT '绑定码过期时间',
    `name` varchar(100) DEFAULT NULL COMMENT '设备名称',
    `model` varchar(100) DEFAULT NULL COMMENT '设备型号',
    `os_version` varchar(50) DEFAULT NULL COMMENT '系统版本',
    `app_version` varchar(50) DEFAULT NULL COMMENT 'APP版本',
    `storage_total` bigint DEFAULT 0 COMMENT '总存储(字节)',
    `storage_used` bigint DEFAULT 0 COMMENT '已用存储(字节)',
    `status` tinyint DEFAULT 0 COMMENT '状态: 0未绑定 1已绑定 2离线',
    `online_at` datetime DEFAULT NULL COMMENT '最后在线时间',
    `bind_at` datetime DEFAULT NULL COMMENT '绑定时间',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_token` (`device_token`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_bind_code` (`bind_code`),
    KEY `idx_status` (`status`),
    KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';

-- =====================================================
-- 素材表
-- =====================================================
CREATE TABLE IF NOT EXISTS `material` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `category_id` bigint DEFAULT NULL COMMENT '分类ID',
    `name` varchar(200) NOT NULL COMMENT '素材名称',
    `original_name` varchar(200) DEFAULT NULL COMMENT '原始文件名',
    `type` varchar(20) NOT NULL COMMENT '类型: image/video/pdf/ppt',
    `original_url` varchar(500) NOT NULL COMMENT '原始文件URL',
    `converted_url` varchar(500) DEFAULT NULL COMMENT '转换后URL',
    `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图URL',
    `file_size` bigint DEFAULT 0 COMMENT '文件大小(字节)',
    `file_md5` varchar(32) DEFAULT NULL COMMENT '文件MD5',
    `duration` int DEFAULT 0 COMMENT '时长(秒)',
    `width` int DEFAULT 0 COMMENT '宽度',
    `height` int DEFAULT 0 COMMENT '高度',
    `page_count` int DEFAULT 0 COMMENT '页数',
    `convert_status` tinyint DEFAULT 0 COMMENT '转码状态: 0无需 1转码中 2成功 3失败',
    `convert_progress` int DEFAULT 0 COMMENT '转码进度(%)',
    `convert_fail_reason` varchar(500) DEFAULT NULL COMMENT '转码失败原因',
    `status` tinyint DEFAULT 1 COMMENT '状态: 1正常 0删除',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='素材表';

-- =====================================================
-- 发布任务表（主表）
-- =====================================================
CREATE TABLE IF NOT EXISTS `publish_task` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `batch_id` varchar(32) DEFAULT NULL COMMENT '批次ID (UUID，用于标识同一次发布的任务)',
    `device_id` bigint NOT NULL COMMENT '设备ID',
    `user_id` bigint NOT NULL COMMENT '创建用户ID',
    `name` varchar(200) DEFAULT NULL COMMENT '任务名称',
    `play_mode` varchar(20) DEFAULT 'SEQUENCE' COMMENT '播放模式: SEQUENCE-顺序播放, RANDOM-随机播放',
    `play_interval` int DEFAULT 5 COMMENT '播放间隔(秒)',
    `loop_play` tinyint DEFAULT 0 COMMENT '循环播放',
    `auto_play` tinyint DEFAULT 1 COMMENT '自动播放',
    `scheduled_at` datetime DEFAULT NULL COMMENT '定时发布时间',
    `published_at` datetime DEFAULT NULL COMMENT '发布时间',
    `started_at` datetime DEFAULT NULL COMMENT '开始播放时间',
    `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
    `status` tinyint DEFAULT 0 COMMENT '状态: 0待发布 1已发布 2播放中 3已完成 4已取消',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_id` (`batch_id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_scheduled_at` (`scheduled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布任务表';

-- =====================================================
-- 发布任务明细表
-- =====================================================
CREATE TABLE IF NOT EXISTS `publish_task_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `task_id` bigint NOT NULL COMMENT '任务ID',
    `material_id` bigint NOT NULL COMMENT '素材ID',
    `sort_order` int DEFAULT 0 COMMENT '播放顺序',
    `duration` int DEFAULT 30 COMMENT '播放时长(秒)',
    `fit_mode` varchar(20) DEFAULT 'FILL' COMMENT '平铺方式: FILL-填充, FIT-适应, ORIGINAL-原始, STRETCH-拉伸',
    `transition` varchar(20) DEFAULT 'CUBE' COMMENT '过渡效果: NONE-无, FADE-淡入淡出, SLIDE-滑动, CUBE-立方体',
    `status` tinyint DEFAULT 0 COMMENT '状态: 0待播放 1播放中 2已完成',
    `play_count` int DEFAULT 0 COMMENT '播放次数',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_material_id` (`material_id`),
    CONSTRAINT `fk_task_item_task` FOREIGN KEY (`task_id`) REFERENCES `publish_task` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布任务明细表';

-- =====================================================
-- 播放日志表
-- =====================================================
CREATE TABLE IF NOT EXISTS `play_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `device_id` bigint NOT NULL COMMENT '设备ID',
    `task_id` bigint DEFAULT NULL COMMENT '任务ID',
    `material_id` bigint DEFAULT NULL COMMENT '素材ID',
    `material_name` varchar(200) DEFAULT NULL COMMENT '素材名称',
    `play_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
    `play_duration` int DEFAULT 0 COMMENT '播放时长(秒)',
    `play_count` int DEFAULT 1 COMMENT '播放次数',
    PRIMARY KEY (`id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_play_at` (`play_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='播放日志表';

-- =====================================================
-- 初始化管理员账号
-- =====================================================
INSERT INTO `user` (`name`, `phone`, `email`, `password`, `status`)
VALUES ('admin', '13800000000', 'admin@example.com', '$2a$10$0cm6oYr/gGEyigYkqgPvUuea3AaTo0k3CG0Jl7IRbvKq2tBhTp6Im', 1);

-- =====================================================
-- 初始化示例分组
-- =====================================================
INSERT INTO `device_group` (`user_id`, `name`, `description`, `sort_order`)
VALUES (1, '一楼展厅', '一楼展厅设备分组', 1);

INSERT INTO `device_group` (`user_id`, `name`, `description`, `sort_order`)
VALUES (1, '二楼展厅', '二楼展厅设备分组', 2);

-- =====================================================
-- End of Script
-- =====================================================

-- =====================================================
-- 设备播放列表表
-- =====================================================
CREATE TABLE IF NOT EXISTS `device_playlist` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `device_id` bigint NOT NULL COMMENT '设备ID',
    `play_mode` varchar(20) DEFAULT 'SEQUENCE' COMMENT '播放模式: SEQUENCE-顺序, RANDOM-随机',
    `play_interval` int DEFAULT 5 COMMENT '播放间隔(秒)',
    `loop_play` tinyint DEFAULT 1 COMMENT '是否循环播放: 1-是, 0-否',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_id` (`device_id`),
    CONSTRAINT `fk_playlist_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备播放列表表';

-- =====================================================
-- 播放项表
-- =====================================================
CREATE TABLE IF NOT EXISTS `playlist_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `playlist_id` bigint NOT NULL COMMENT '播放列表ID',
    `material_id` bigint NOT NULL COMMENT '素材ID',
    `sort_order` int DEFAULT 0 COMMENT '排序序号',
    `fit_mode` varchar(20) DEFAULT 'FILL' COMMENT '平铺方式: FILL-填充, FIT-适应, ORIGINAL-原始, STRETCH-拉伸',
    `duration` int DEFAULT 10 COMMENT '播放时长(秒) - 图片/PDF专用',
    `transition` varchar(20) DEFAULT 'NONE' COMMENT '过渡效果: NONE-无, FADE-淡入淡出, SLIDE-滑动',
    PRIMARY KEY (`id`),
    KEY `idx_playlist_id` (`playlist_id`),
    KEY `idx_material_id` (`material_id`),
    CONSTRAINT `fk_item_playlist` FOREIGN KEY (`playlist_id`) REFERENCES `device_playlist` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_item_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='播放项表';
