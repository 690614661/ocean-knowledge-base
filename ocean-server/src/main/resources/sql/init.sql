-- 海洋生物知识库 数据库初始化脚本
-- Database: ocean_knowledge

CREATE DATABASE IF NOT EXISTS ocean_knowledge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ocean_knowledge;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL COMMENT '用户ID（雪花算法）',
    `login_name` VARCHAR(50) NOT NULL COMMENT '登录名',
    `name` VARCHAR(50) NOT NULL COMMENT '昵称',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（MD5+盐值）',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin/user',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_login_name` (`login_name`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL COMMENT '分类ID（雪花算法）',
    `parent` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，顶级为0',
    `name` VARCHAR(20) NOT NULL COMMENT '分类名称',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，升序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 电子书表
CREATE TABLE IF NOT EXISTS `ebook` (
    `id` BIGINT NOT NULL COMMENT '电子书ID（雪花算法）',
    `name` VARCHAR(50) NOT NULL COMMENT '电子书名称',
    `category1_id` BIGINT NOT NULL COMMENT '一级分类ID',
    `category2_id` BIGINT NOT NULL COMMENT '二级分类ID',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `cover` VARCHAR(200) DEFAULT NULL COMMENT '封面图片路径',
    `doc_count` INT NOT NULL DEFAULT 0 COMMENT '文档总数',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '总阅读数',
    `vote_count` INT NOT NULL DEFAULT 0 COMMENT '总点赞数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category1` (`category1_id`),
    KEY `idx_category2` (`category2_id`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子书表';

-- 文档表
CREATE TABLE IF NOT EXISTS `doc` (
    `id` BIGINT NOT NULL COMMENT '文档ID（雪花算法）',
    `ebook_id` BIGINT NOT NULL COMMENT '所属电子书ID',
    `parent` BIGINT NOT NULL DEFAULT 0 COMMENT '父文档ID，顶级为0',
    `name` VARCHAR(50) NOT NULL COMMENT '文档名称',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '阅读数',
    `vote_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ebook_id` (`ebook_id`),
    KEY `idx_parent` (`parent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 文档内容表
CREATE TABLE IF NOT EXISTS `content` (
    `id` BIGINT NOT NULL COMMENT '文档ID（与doc.id一致）',
    `content` MEDIUMTEXT DEFAULT NULL COMMENT '富文本HTML内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档内容表';

-- 电子书数据快照表
CREATE TABLE IF NOT EXISTS `ebook_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `ebook_id` BIGINT NOT NULL COMMENT '电子书ID',
    `date` DATE NOT NULL COMMENT '快照日期',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '当日总阅读数',
    `vote_count` INT NOT NULL DEFAULT 0 COMMENT '当日总点赞数',
    `view_increase` INT NOT NULL DEFAULT 0 COMMENT '当日阅读增量',
    `vote_increase` INT NOT NULL DEFAULT 0 COMMENT '当日点赞增量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ebook_date` (`ebook_id`, `date`),
    KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子书数据快照表';

-- 用户笔记表
CREATE TABLE IF NOT EXISTS `note` (
    `id` BIGINT NOT NULL COMMENT '笔记ID（雪花算法）',
    `user_id` BIGINT NOT NULL COMMENT '作者ID',
    `title` VARCHAR(100) NOT NULL COMMENT '笔记标题',
    `content` MEDIUMTEXT DEFAULT NULL COMMENT '富文本HTML内容',
    `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开（0私有 1公开）',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '阅读数',
    `vote_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_public` (`is_public`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户笔记表';

-- AI 对话会话表
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` VARCHAR(255) NOT NULL COMMENT '会话ID（UUID）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '会话标题',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- AI 对话消息表
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id` VARCHAR(255) NOT NULL COMMENT '消息ID（UUID）',
    `conversation_id` VARCHAR(255) NOT NULL COMMENT '会话ID',
    `role` VARCHAR(10) NOT NULL COMMENT '消息角色：user/assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `prompt_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入token数',
    `completion_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出token数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- AI 用量日志表
CREATE TABLE IF NOT EXISTS `ai_usage_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `feature` VARCHAR(20) NOT NULL COMMENT '功能类型：chat/note/doc_assist',
    `provider` VARCHAR(20) NOT NULL COMMENT '供应商名称',
    `model` VARCHAR(50) NOT NULL COMMENT '模型名称',
    `prompt_tokens` INT NOT NULL COMMENT '输入token数',
    `completion_tokens` INT NOT NULL COMMENT '输出token数',
    `total_tokens` INT NOT NULL COMMENT '总token数',
    `cost_yuan` DECIMAL(10,6) NOT NULL COMMENT '费用（元）',
    `latency_ms` INT NOT NULL COMMENT '响应耗时（毫秒）',
    `status` VARCHAR(10) NOT NULL COMMENT '调用状态：success/error',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI用量日志表';

-- 用户收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT NOT NULL COMMENT '收藏ID（雪花算法）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `doc_id` BIGINT NOT NULL COMMENT '文档ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_doc` (`user_id`, `doc_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- doc 表新增收藏数字段
ALTER TABLE `doc` ADD COLUMN `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数' AFTER `vote_count`;

-- 初始化管理员账号（密码: admin123，盐值: ocean_knowledge_salt_2026）
-- MD5(admin123 + ocean_knowledge_salt_2026) = 需要程序计算
INSERT INTO `user` (`id`, `login_name`, `name`, `password`, `role`) VALUES
(1, 'admin', '管理员', '9ee22bd44711c82f309c1c12d3ac3912', 'admin');
