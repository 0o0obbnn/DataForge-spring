-- DataForge 数据库迁移脚本
-- 版本: V1.2
-- 描述: 更新用户表结构，添加安全相关字段

-- 添加用户表缺失的字段
ALTER TABLE users
ADD COLUMN IF NOT EXISTS full_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'USER',
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS login_attempts INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS lock_time TIMESTAMP;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_lock_time ON users(lock_time);

-- 插入默认管理员用户（密码: admin123，使用BCrypt加密）
-- 注意：生产环境应该使用环境变量或安全方式设置初始密码
INSERT INTO users (
    username,
    password,
    email,
    full_name,
    role,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at,
    login_attempts
) VALUES (
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', -- admin123
    'admin@dataforge.com',
    'System Administrator',
    'ADMIN',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    role = VALUES(role),
    enabled = VALUES(enabled),
    updated_at = CURRENT_TIMESTAMP;

-- 插入默认普通用户（密码: user123，使用BCrypt加密）
INSERT INTO users (
    username,
    password,
    email,
    full_name,
    role,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    updated_at,
    login_attempts
) VALUES (
    'user',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', -- user123 (same hash for demo)
    'user@dataforge.com',
    'Normal User',
    'USER',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    role = VALUES(role),
    enabled = VALUES(enabled),
    updated_at = CURRENT_TIMESTAMP;
