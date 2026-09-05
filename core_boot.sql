/*
 Navicat Premium Data Transfer

 Source Server         : mysql5.7
 Source Server Type    : MySQL
 Source Server Version : 50726 (5.7.26)
 Source Host           : localhost:3306
 Source Schema         : core_boot

 Target Server Type    : MySQL
 Target Server Version : 50726 (5.7.26)
 File Encoding         : 65001

 Date: 24/08/2026 07:50:53
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_cart
-- ----------------------------
DROP TABLE IF EXISTS `t_cart`;
CREATE TABLE `t_cart`  (
  `id` int(64) NOT NULL AUTO_INCREMENT COMMENT '购物车id',
  `user_id` int(11) NOT NULL COMMENT '用户id',
  `product_id` int(11) NOT NULL COMMENT '商品id',
  `quantity` int(11) NOT NULL COMMENT '商品数量',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_product`(`user_id`, `product_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cart
-- ----------------------------

-- ----------------------------
-- Table structure for t_category
-- ----------------------------
DROP TABLE IF EXISTS `t_category`;
CREATE TABLE `t_category`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '分类id',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `rank` int(11) NOT NULL COMMENT '分类等级，不超过3级',
  `order` int(11) NOT NULL COMMENT '分类排序',
  `parent_id` int(11) NOT NULL COMMENT '父分类id，一级目录为0',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_category
-- ----------------------------
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (1, '新鲜水果', 1, 1, 0, '2025-04-11 16:45:36', '2025-04-12 19:04:24');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (2, '苹果', 2, 2, 1, '2025-04-11 16:45:36', '2025-04-12 19:05:12');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (3, '梨子', 2, 3, 1, '2025-04-11 16:45:36', '2025-04-12 19:05:15');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (4, '红富士苹果', 3, 4, 2, '2025-04-11 16:45:36', '2025-04-12 19:05:42');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (5, '红星苹果', 3, 5, 2, '2025-04-11 16:45:36', '2025-04-12 19:06:43');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (6, '鸭梨', 3, 6, 3, '2025-04-11 16:45:36', '2025-04-12 19:07:26');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (7, '秋月梨', 3, 7, 3, '2025-04-11 16:45:36', '2025-04-12 19:07:45');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (8, '蔬菜', 1, 8, 0, '2025-04-11 16:45:36', '2025-04-12 19:08:28');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (9, '白菜', 2, 9, 8, '2025-04-11 16:45:36', '2025-04-12 19:08:50');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (10, '蒜叶', 2, 10, 8, '2025-04-11 16:45:36', '2025-04-12 19:09:11');
INSERT INTO `t_category` (`id`, `name`, `rank`, `order`, `parent_id`, `create_time`, `update_time`) VALUES (11, '茄子', 2, 11, 8, '2025-04-11 16:45:36', '2025-04-12 19:09:28');

-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`  (
  `id` int(64) NOT NULL AUTO_INCREMENT COMMENT '商品id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品图片',
  `detail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品详情',
  `category_id` int(11) NOT NULL COMMENT '分类id',
  `price` int(11) NOT NULL COMMENT '价格，单位-分',
  `stock` int(11) NOT NULL COMMENT '库存数量',
  `sale` int(11) NOT NULL DEFAULT 0 COMMENT '真实销量',
  `virtual_sale` int(11) NOT NULL DEFAULT 0 COMMENT '虚拟销量',
  `total_sale` int(11) GENERATED ALWAYS AS ((`sale` + `virtual_sale`)) STORED COMMENT '总销量',
  `status` int(6) NOT NULL COMMENT '商品状态，0-下架，1-上架',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_status`(`category_id`, `status`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_product
-- ----------------------------
INSERT INTO `t_product` (`id`, `name`, `image`, `detail`, `category_id`, `price`, `stock`, `status`, `create_time`, `update_time`) VALUES (1, '红富士苹果1', '/images/ce15d63a-a854-41b2-a625-483b7549fdc5.jpg', '新鲜又美味', 4, 33, 100, 1, '2025-04-13 00:55:31', '2025-04-14 01:45:45');
INSERT INTO `t_product` (`id`, `name`, `image`, `detail`, `category_id`, `price`, `stock`, `status`, `create_time`, `update_time`) VALUES (2, '红富士苹果2', '/images/ce15d63a-a854-41b2-a625-483b7549fdc5.jpg', '新鲜又美味', 4, 66, 100, 1, '2025-04-13 00:55:31', '2025-04-15 01:46:20');
INSERT INTO `t_product` (`id`, `name`, `image`, `detail`, `category_id`, `price`, `stock`, `status`, `create_time`, `update_time`) VALUES (3, '红星苹果1', '/images/ce15d63a-a854-41b2-a625-483b7549fdc5.jpg', '新鲜又美味', 5, 77, 100, 1, '2025-04-13 00:55:31', '2025-04-16 01:45:45');
INSERT INTO `t_product` (`id`, `name`, `image`, `detail`, `category_id`, `price`, `stock`, `status`, `create_time`, `update_time`) VALUES (4, '红星苹果2', '/images/ce15d63a-a854-41b2-a625-483b7549fdc5.jpg', '新鲜又美味', 5, 88, 100, 1, '2025-04-13 00:55:31', '2025-04-17 01:45:45');
INSERT INTO `t_product` (`id`, `name`, `image`, `detail`, `category_id`, `price`, `stock`, `status`, `create_time`, `update_time`) VALUES (5, '秋月梨1', '/images/ce15d63a-a854-41b2-a625-483b7549fdc5.jpg', '新鲜又美味', 7, 99, 100, 1, '2025-04-13 00:55:31', '2025-04-18 01:45:45');
INSERT INTO `t_product` (`id`, `name`, `image`, `detail`, `category_id`, `price`, `stock`, `status`, `create_time`, `update_time`) VALUES (6, '秋月梨2', '/images/ce15d63a-a854-41b2-a625-483b7549fdc5.jpg', '新鲜又美味', 7, 100, 100, 1, '2025-04-13 00:55:31', '2025-04-19 01:45:45');
INSERT INTO `t_product` (`id`, `name`, `image`, `detail`, `category_id`, `price`, `stock`, `status`, `create_time`, `update_time`) VALUES (7, '白菜1', '/images/ce15d63a-a854-41b2-a625-483b7549fdc5.jpg', '新鲜又美味', 9, 111, 100, 1, '2025-04-13 00:55:31', '2025-04-20 01:45:45');

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
  `id` int(64) NOT NULL AUTO_INCREMENT COMMENT '订单id',
  `user_id` int(11) NOT NULL COMMENT '用户id',
  `order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单编号',
  `order_status` int(10) NOT NULL COMMENT '订单状态，0-已取消，10-未付款，20-已付款，30-已发货，40-已完成',
  `total_price` int(64) NOT NULL COMMENT '订单总价',
  `receiver_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '收货人姓名快照',
  `receiver_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '收货人手机号快照',
  `receiver_address` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '收货人地址快照',
  `postage` int(11) NOT NULL DEFAULT 0 COMMENT '运费',
  `payment_type` int(4) NOT NULL DEFAULT 1 COMMENT '支付类型，1-在线支付',
  `delivery_time` timestamp NULL DEFAULT NULL COMMENT '发货时间',
  `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '完成时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no`) USING BTREE,
  INDEX `idx_user_create_time`(`user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_order
-- ----------------------------

-- ----------------------------
-- Table structure for t_order_item
-- ----------------------------
DROP TABLE IF EXISTS `t_order_item`;
CREATE TABLE `t_order_item`  (
  `id` int(64) NOT NULL AUTO_INCREMENT COMMENT '订单物品id',
  `order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '归属订单编号',
  `product_id` int(11) NOT NULL COMMENT '商品id',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `product_image` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品图片',
  `unit_price` int(11) NOT NULL COMMENT '商品单价快照',
  `quantity` int(11) NOT NULL COMMENT '商品数量',
  `total_price` int(11) NOT NULL COMMENT '商品总价快照',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_no`(`order_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_order_item
-- ----------------------------

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` int(64) NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户密码，MD5加密',
  `signature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户签名',
  `role` int(4) NOT NULL DEFAULT 1 COMMENT '用户角色，1-普通用户，2-管理员',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` (`id`, `name`, `password`, `signature`, `role`, `create_time`, `update_time`) VALUES (1, '张三', 'd1+gq0CDoKCTzrUw7drCEw==', '天气晴朗', 1, '2025-04-03 11:24:07', '2025-04-11 01:09:13');
INSERT INTO `t_user` (`id`, `name`, `password`, `signature`, `role`, `create_time`, `update_time`) VALUES (2, '李四', 'd1+gq0CDoKCTzrUw7drCEw==', '欢迎，欢迎', 2, '2025-04-03 11:24:41', '2025-04-11 01:08:23');
INSERT INTO `t_user` (`id`, `name`, `password`, `signature`, `role`, `create_time`, `update_time`) VALUES (3, '王二', 'd1+gq0CDoKCTzrUw7drCEw==', NULL, 1, '2025-04-04 02:57:19', '2025-04-11 01:08:23');
INSERT INTO `t_user` (`id`, `name`, `password`, `signature`, `role`, `create_time`, `update_time`) VALUES (4, '叶子', 'd1+gq0CDoKCTzrUw7drCEw==', NULL, 1, '2025-04-10 16:56:28', '2025-04-10 16:56:28');
INSERT INTO `t_user` (`id`, `name`, `password`, `signature`, `role`, `create_time`, `update_time`) VALUES (5, '用户', 'd1+gq0CDoKCTzrUw7drCEw==', NULL, 1, '2025-04-10 16:57:06', '2025-04-10 16:57:06');
INSERT INTO `t_user` (`id`, `name`, `password`, `signature`, `role`, `create_time`, `update_time`) VALUES (6, '用户1', 'd1+gq0CDoKCTzrUw7drCEw==', '天气晴朗好耶', 1, '2025-04-11 01:40:38', '2025-04-11 01:41:35');

SET FOREIGN_KEY_CHECKS = 1;
