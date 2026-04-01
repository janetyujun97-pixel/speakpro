-- 003_analytics_table.sql
-- 数据埋点表

CREATE TABLE IF NOT EXISTS analytics_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_name VARCHAR(100) NOT NULL,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  payload JSONB DEFAULT '{}',
  ip_address VARCHAR(45),
  user_agent TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 按事件名称 + 时间查询
CREATE INDEX IF NOT EXISTS idx_analytics_event_name_time
ON analytics_events (event_name, created_at DESC);

-- 按用户 + 时间查询
CREATE INDEX IF NOT EXISTS idx_analytics_user_time
ON analytics_events (user_id, created_at DESC);

-- 按时间范围聚合（DAU/MAU）
CREATE INDEX IF NOT EXISTS idx_analytics_created_at
ON analytics_events (created_at);
