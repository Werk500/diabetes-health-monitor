-- save_chat_context.lua
-- KEYS[1] = chat:context:{userId}
-- ARGV[1] = 序列化后的消息 JSON
-- ARGV[2] = 最大保留条数
-- ARGV[3] = 过期时间（秒）

redis.call('RPUSH', KEYS[1], ARGV[1])
redis.call('LTRIM', KEYS[1], -ARGV[2], -1)
redis.call('EXPIRE', KEYS[1], ARGV[3])

-- 返回当前列表长度，方便监控
return redis.call('LLEN', KEYS[1])