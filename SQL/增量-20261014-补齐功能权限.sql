-- ============================================================
-- 增量脚本：补齐缺失的功能权限（2026-10 安全加固配套）
--
-- 背景：UserController / LoginHistoryController / ToolController 原先没有任何
--       @PreAuthorize 权限校验，只要登录成功就能增删改用户、删除登录历史、生成 DDL。
--       加固后已补上权限注解，因此必须在库中同步补齐对应的权限记录，
--       并把这些权限授予超级管理员角色，否则会出现"加了权限就全员 403"。
--
-- 适用范围：已经执行过 lapis_admin_dev.sql 的存量库。
--           全新部署直接导入 lapis_admin_dev.sql 即可，无需执行本文件。
-- ============================================================

SET NAMES utf8mb4;

-- 1) 新增权限记录（parent_id：36=用户管理，4=登录历史，1=系统管理）
INSERT INTO `hyn_sys_authority` (`id`, `parent_id`, `is_leaf`, `is_builtin`, `level`, `authority_type`, `authority_code`, `authority_name`, `icon`, `path`, `sort`, `remark`, `created_by`, `created_time`, `last_updated_by`, `last_updated_time`) VALUES
(93, 36, 1, 1, 3, 'button', 'sys-user:add',            '新增用户',           'plus',   '', 2, '', 1, NOW(), 1, NOW()),
(94, 36, 1, 1, 3, 'button', 'sys-user:update',         '修改用户',           'edit',   '', 3, '', 1, NOW(), 1, NOW()),
(95, 36, 1, 1, 3, 'button', 'sys-user:delete',         '删除用户',           'delete', '', 4, '', 1, NOW(), 1, NOW()),
(96, 36, 1, 1, 3, 'button', 'sys-user:reset-password', '重置密码',           'unlock', '', 5, '', 1, NOW(), 1, NOW()),
(97,  4, 1, 1, 3, 'button', 'sys-login-history:delete','删除登录历史',       'delete', '', 2, '', 1, NOW(), 1, NOW()),
(98,  1, 1, 1, 2, 'button', 'sys-tool:generate-ddl',   '生成逻辑删除归档表', 'tool',   '', 7, '', 1, NOW(), 1, NOW())
ON DUPLICATE KEY UPDATE `authority_name` = VALUES(`authority_name`);

-- 2) 授予超级管理员角色（role_id = 1，SUPER_ADMIN）
INSERT INTO `hyn_sys_role_authority` (`role_id`, `authority_id`, `created_by`, `created_time`) VALUES
(1, 93, 1, NOW()),
(1, 94, 1, NOW()),
(1, 95, 1, NOW()),
(1, 96, 1, NOW()),
(1, 97, 1, NOW()),
(1, 98, 1, NOW())
ON DUPLICATE KEY UPDATE `created_time` = VALUES(`created_time`);

-- 3) 说明：如果库中还有其他自建角色也需要这些权限，请按同样方式补 hyn_sys_role_authority 记录。
