-- 短信验证码发送 Lua 脚本
-- KEYS[1] = sms:limit:{phone}  限流Key
-- KEYS[2] = sms:code:{phone}   验证码Key
-- ARGV[1] = 验证码
-- ARGV[2] = 验证码TTL秒
-- ARGV[3] = 限流TTL秒

-- 1. 检查限流Key是否存在
local limitKey = KEYS[1]
local codeKey = KEYS[2]

local exists = redis.call('exists', limitKey)
if exists == 1 then
    -- 限流中，返回剩余秒数（负数表示还在限流）
    local ttl = redis.call('ttl', limitKey)
    return 'LIMIT:' .. ttl
end

-- 2. 存储验证码
redis.call('setex', codeKey, tonumber(ARGV[2]), ARGV[1])

-- 3. 设置限流标记
redis.call('setex', limitKey, tonumber(ARGV[3]), '1')

-- 4. 返回成功
return 'OK'