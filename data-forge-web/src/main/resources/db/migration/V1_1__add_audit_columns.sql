-- DataForge 数据库迁移脚本
-- 版本: V1.1
-- 描述: 为表添加审计字段

-- 为 data_templates 表添加更多审计字段
ALTER TABLE data_templates
ADD COLUMN IF NOT EXISTS usage_count INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_data_templates_usage_count ON data_templates(usage_count);

-- 为 generation_history 表添加更多统计字段
ALTER TABLE generation_history
ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT,
ADD COLUMN IF NOT EXISTS throughput_records_per_sec NUMERIC(10,2),
ADD COLUMN IF NOT EXISTS thread_count INT DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_generation_history_throughput ON generation_history(throughput_records_per_sec);

-- 创建生成历史统计视图
CREATE OR REPLACE VIEW generation_statistics AS
SELECT 
    created_at::date as date,
    COUNT(*) as total_tasks,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_tasks,
    SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress_tasks,
    SUM(record_count) as total_records,
    AVG(duration_ms) as avg_duration_ms,
    AVG(throughput_records_per_sec) as avg_throughput
FROM generation_history
GROUP BY created_at::date
ORDER BY date DESC;
