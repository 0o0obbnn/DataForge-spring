-- DataForge 数据库迁移脚本
-- 版本: V1.3
-- 描述: 初始化 admin / tester 账号（幂等）

-- admin / tester 账号密码采用 BCrypt 哈希：
-- admin  -> admin123456*
-- tester -> test123456*

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
    login_attempts,
    lock_time
) VALUES (
    'admin',
    '$2y$10$MRLYYyUbf7bzcp0sBda9fOKpUouzBloZTWVnBQAwvMojgFP.FkONS',
    'admin@dataforge.com',
    'System Administrator',
    'ADMIN',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0,
    NULL
)
ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    enabled = TRUE,
    account_non_expired = TRUE,
    account_non_locked = TRUE,
    credentials_non_expired = TRUE,
    login_attempts = 0,
    lock_time = NULL,
    updated_at = CURRENT_TIMESTAMP;

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
    login_attempts,
    lock_time
) VALUES (
    'tester',
    '$2y$10$iilNe7wDSLLeTbZCkh0J5.MGZbFuf.rEcpVosvRpbjkyLxTTwytre',
    'tester@dataforge.com',
    'Test User',
    'USER',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0,
    NULL
)
ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    enabled = TRUE,
    account_non_expired = TRUE,
    account_non_locked = TRUE,
    credentials_non_expired = TRUE,
    login_attempts = 0,
    lock_time = NULL,
    updated_at = CURRENT_TIMESTAMP;

