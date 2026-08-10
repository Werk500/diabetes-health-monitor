-- 短信验证码校验 Lua 脚本
-- KEYS[1] = sms:code:{phone}  验证码Key
-- ARGV[1] = 用户输入的验证码

local codeKey = KEYS[1]
local userCode = ARGV[1]

-- 1. 获取存储的验证码
local storedCode = redis.call('get', codeKey)

-- 2. 验证码不存在或已过期
if storedCode == false or storedCode == nil then
    return 'EXPIRED'
end

-- 3. 验证码不匹配
if storedCode ~= userCode then
    return 'WRONG'
end

-- 4. 验证码匹配成功，删除（一次性使用）
redis.call('del', codeKey)

-- 5. 返回成功
return 'OK'