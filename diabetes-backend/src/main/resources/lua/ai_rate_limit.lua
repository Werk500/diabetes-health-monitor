-- AI接口固定窗口限流脚本
-- KEYS[1] = 限流key，例如 ai:rate:1:/api/ai/chat
-- ARGV[1] = 窗口内最大请求数
-- ARGV[2] = 窗口时间，单位：秒
-- 返回当前请求计数；超过限制时返回 -1

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])

local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, window)
end

if current > limit then
    return -1
end

return current