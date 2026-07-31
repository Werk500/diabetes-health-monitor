-- ============================================
-- 糖尿病人健康监测系统 - 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS diabetes_monitor DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE diabetes_monitor;

-- 1. 用户表
CREATE TABLE sys_user (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    gender TINYINT DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
    age INT COMMENT '年龄',
    height DECIMAL(5,2) COMMENT '身高(cm)',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    role TINYINT DEFAULT 0 COMMENT '角色 0-普通用户 1-管理员',
    diabetes_type TINYINT COMMENT '糖尿病类型 1-1型 2-2型 3-妊娠期',
    diagnosed_date DATE COMMENT '确诊日期',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 身体指标记录表
CREATE TABLE health_record_body (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    weight DECIMAL(5,2) COMMENT '体重(kg)',
    bmi DECIMAL(4,1) COMMENT 'BMI指数',
    body_fat DECIMAL(4,1) COMMENT '体脂率(%)',
    systolic_pressure INT COMMENT '收缩压(mmHg)',
    diastolic_pressure INT COMMENT '舒张压(mmHg)',
    heart_rate INT COMMENT '心率(次/分钟)',
    waistline DECIMAL(5,1) COMMENT '腰围(cm)',
    record_date DATE NOT NULL COMMENT '记录日期',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身体指标记录表';

-- 3. 血糖记录表
CREATE TABLE health_record_blood_sugar (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    blood_sugar DECIMAL(4,1) NOT NULL COMMENT '血糖值(mmol/L)',
    measure_type TINYINT NOT NULL COMMENT '测量类型 1-空腹 2-餐前 3-餐后2h 4-睡前 5-凌晨',
    measure_time DATETIME NOT NULL COMMENT '测量时间',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='血糖记录表';

-- 4. 饮食记录表
CREATE TABLE health_record_diet (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    food_name VARCHAR(100) NOT NULL COMMENT '食物名称',
    meal_type TINYINT NOT NULL COMMENT '餐次 1-早餐 2-午餐 3-晚餐 4-加餐',
    calories DECIMAL(7,2) COMMENT '热量(kcal)',
    carbs DECIMAL(6,2) COMMENT '碳水化合物(g)',
    protein DECIMAL(6,2) COMMENT '蛋白质(g)',
    fat DECIMAL(6,2) COMMENT '脂肪(g)',
    fiber DECIMAL(6,2) COMMENT '膳食纤维(g)',
    portion DECIMAL(5,1) COMMENT '份量(g)',
    eat_time DATETIME NOT NULL COMMENT '进食时间',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食记录表';

-- 5. 运动类型表
CREATE TABLE sys_exercise_type (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '类型ID',
    type_name VARCHAR(50) NOT NULL COMMENT '运动名称',
    calories_per_hour DECIMAL(6,2) COMMENT '每小时消耗热量(kcal)',
    intensity TINYINT COMMENT '强度 1-低 2-中 3-高',
    suitable_for VARCHAR(255) COMMENT '适用人群说明',
    description TEXT COMMENT '运动描述',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动类型表';

-- 6. 运动记录表
CREATE TABLE health_record_exercise (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    exercise_type_id INT COMMENT '运动类型ID',
    duration_minutes INT NOT NULL COMMENT '运动时长(分钟)',
    calories_burned DECIMAL(6,2) COMMENT '消耗热量(kcal)',
    heart_rate_avg INT COMMENT '平均心率',
    exercise_date DATE NOT NULL COMMENT '运动日期',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_type_id) REFERENCES sys_exercise_type(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动记录表';

-- 7. 健康文章表
CREATE TABLE health_article (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID',
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    content TEXT NOT NULL COMMENT '文章内容',
    summary VARCHAR(500) COMMENT '文章摘要',
    category TINYINT NOT NULL COMMENT '分类 1-血糖监测技巧 2-控糖饮食指南 3-并发症预防 4-运动建议',
    cover_image VARCHAR(255) COMMENT '封面图片',
    author VARCHAR(50) COMMENT '作者',
    push_status TINYINT DEFAULT 0 COMMENT '推送状态 0-未推送 1-已推送',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    status TINYINT DEFAULT 1 COMMENT '状态 0-下架 1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康文章表';

-- 8. 文章推送记录表
CREATE TABLE sys_user_article (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    article_id INT NOT NULL COMMENT '文章ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
    read_time DATETIME COMMENT '阅读时间',
    push_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES health_article(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章推送记录表';

-- ============================================
-- 初始数据
-- ============================================

-- 管理员账号 admin / 123456
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1, 1);

-- 测试用户
INSERT INTO sys_user (username, password, real_name, gender, age, height, role, diabetes_type) VALUES
('test001', 'e10adc3949ba59abbe56e057f20f883e', '张三', 1, 45, 172.00, 0, 2);

-- 运动类型初始数据
INSERT INTO sys_exercise_type (type_name, calories_per_hour, intensity, suitable_for, description) VALUES
('散步', 180.00, 1, '所有糖尿病患者', '低强度有氧运动，适合饭后30分钟进行，每次30-45分钟为宜'),
('慢跑', 400.00, 2, '血糖控制较好者', '中等强度有氧运动，注意监测血糖，避免低血糖'),
('太极拳', 280.00, 1, '中老年患者', '柔和缓慢，有助于改善胰岛素敏感性'),
('游泳', 500.00, 2, '无并发症患者', '全身性运动，对关节压力小'),
('骑自行车', 350.00, 2, '大部分患者', '中等强度，注意安全防护'),
('瑜伽', 250.00, 1, '血糖稳定者', '有助于减压和改善血糖控制'),
('力量训练', 420.00, 3, '无严重并发症者', '增加肌肉量，提高基础代谢和胰岛素敏感性'),
('快走', 300.00, 2, '所有患者', '中等强度，简单易行的有氧运动');

-- 健康文章初始数据
INSERT INTO health_article (title, content, summary, category, author, push_status, status) VALUES
('血糖自我监测的正确方法', '血糖自我监测是糖尿病管理的重要组成部分。建议空腹血糖控制在4.4-7.0mmol/L，餐后2小时血糖控制在<10.0mmol/L。每天监测频率应根据治疗方案和血糖控制情况而定：使用胰岛素治疗者建议每天监测3-4次，口服药治疗血糖达标者可每周监测2-4次。', '掌握正确的血糖监测频率和方法，是糖尿病管理的第一步。', 1, '健康管理师', 1, 1),
('糖尿病饮食五指法：一餐该吃多少', '控糖饮食并不复杂，用"手掌法则"轻松掌握：主食每餐一个拳头大小（约50-75g生重），蛋白质一个掌心大小（约50-100g），蔬菜双手捧起（约300-500g），油脂一个拇指尖大小（约10-15g），水果一个拳头大小（血糖达标时可适量）。', '用手掌法则轻松控制每餐份量，控糖饮食不再难。', 2, '营养师', 1, 1),
('糖尿病常见并发症及早期预警信号', '糖尿病并发症分为急性和慢性两大类。急性并发症包括低血糖、酮症酸中毒等；慢性并发症累及心脑血管、肾脏、眼底、神经和足部。出现以下症状需警惕：视力模糊、四肢麻木刺痛、泡沫尿、足部溃疡不愈合、胸闷气短等。定期体检是预防并发症的关键。', '了解并发症早期信号，做到早发现、早干预、早治疗。', 3, '内分泌科医师', 1, 1),
('适合糖尿病患者的运动指南', '运动是糖尿病治疗的"五驾马车"之一。推荐每周至少150分钟中等强度有氧运动（如快走、骑车），每周2-3次抗阻训练。运动前后需监测血糖：血糖<5.6mmol/L应补充碳水，血糖>16.7mmol/L应暂缓运动。最佳运动时间为餐后1小时左右。', '科学运动降血糖，这份运动指南请收好。', 4, '运动康复师', 1, 1);

