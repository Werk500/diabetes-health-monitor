/**
 * 示例数据 - 模拟一位2型糖尿病患者最近一周的健康数据
 * 所有数据均为模拟数据，仅用于界面展示和用户体验提升
 */

// 获取最近一周的日期字符串列表
const getRecentWeek = () => {
  const dates = [];
  const now = new Date();
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    dates.push(d.toISOString().split('T')[0]);
  }
  return dates;
};

const weekDates = getRecentWeek();

// ==================== 身体指标示例数据 ====================
export const demoBodyRecords = [
  { id: 101, userId: 1, weight: 78.5, bmi: 26.5, bodyFat: 28.3, systolicPressure: 138, diastolicPressure: 88, heartRate: 78, waistline: 92.0, recordDate: weekDates[0], remark: null, createTime: null },
  { id: 102, userId: 1, weight: 78.2, bmi: 26.4, bodyFat: 28.1, systolicPressure: 135, diastolicPressure: 86, heartRate: 76, waistline: 91.5, recordDate: weekDates[1], remark: null, createTime: null },
  { id: 103, userId: 1, weight: 78.0, bmi: 26.3, bodyFat: 27.9, systolicPressure: 132, diastolicPressure: 85, heartRate: 74, waistline: 91.0, recordDate: weekDates[2], remark: null, createTime: null },
  { id: 104, userId: 1, weight: 77.8, bmi: 26.2, bodyFat: 27.7, systolicPressure: 130, diastolicPressure: 84, heartRate: 73, waistline: 90.8, recordDate: weekDates[3], remark: null, createTime: null },
  { id: 105, userId: 1, weight: 77.5, bmi: 26.1, bodyFat: 27.5, systolicPressure: 128, diastolicPressure: 83, heartRate: 72, waistline: 90.5, recordDate: weekDates[4], remark: null, createTime: null },
  { id: 106, userId: 1, weight: 77.3, bmi: 26.0, bodyFat: 27.3, systolicPressure: 126, diastolicPressure: 82, heartRate: 71, waistline: 90.2, recordDate: weekDates[5], remark: null, createTime: null },
  { id: 107, userId: 1, weight: 77.0, bmi: 25.9, bodyFat: 27.0, systolicPressure: 125, diastolicPressure: 81, heartRate: 70, waistline: 90.0, recordDate: weekDates[6], remark: null, createTime: null },
];

// ==================== 血糖记录示例数据 ====================
export const demoBloodSugarRecords = [
  { id: 201, userId: 1, bloodSugar: 6.8, measureType: 1, measureTime: `${weekDates[0]} 07:30:00`, remark: null, createTime: null },
  { id: 202, userId: 1, bloodSugar: 9.2, measureType: 3, measureTime: `${weekDates[0]} 09:30:00`, remark: '早餐后2h', createTime: null },
  { id: 203, userId: 1, bloodSugar: 5.8, measureType: 2, measureTime: `${weekDates[0]} 12:00:00`, remark: null, createTime: null },
  { id: 204, userId: 1, bloodSugar: 7.1, measureType: 4, measureTime: `${weekDates[0]} 22:00:00`, remark: null, createTime: null },
  { id: 205, userId: 1, bloodSugar: 6.5, measureType: 1, measureTime: `${weekDates[1]} 07:30:00`, remark: null, createTime: null },
  { id: 206, userId: 1, bloodSugar: 8.8, measureType: 3, measureTime: `${weekDates[1]} 09:30:00`, remark: '早餐后2h', createTime: null },
  { id: 207, userId: 1, bloodSugar: 5.6, measureType: 2, measureTime: `${weekDates[2]} 11:30:00`, remark: null, createTime: null },
  { id: 208, userId: 1, bloodSugar: 6.3, measureType: 1, measureTime: `${weekDates[3]} 07:30:00`, remark: null, createTime: null },
  { id: 209, userId: 1, bloodSugar: 8.5, measureType: 3, measureTime: `${weekDates[3]} 09:30:00`, remark: '早餐后2h', createTime: null },
  { id: 210, userId: 1, bloodSugar: 6.8, measureType: 4, measureTime: `${weekDates[3]} 22:00:00`, remark: null, createTime: null },
  { id: 211, userId: 1, bloodSugar: 6.1, measureType: 1, measureTime: `${weekDates[4]} 07:30:00`, remark: null, createTime: null },
  { id: 212, userId: 1, bloodSugar: 7.9, measureType: 3, measureTime: `${weekDates[4]} 09:30:00`, remark: '早餐后2h', createTime: null },
  { id: 213, userId: 1, bloodSugar: 5.9, measureType: 1, measureTime: `${weekDates[5]} 07:30:00`, remark: null, createTime: null },
  { id: 214, userId: 1, bloodSugar: 7.5, measureType: 3, measureTime: `${weekDates[5]} 09:30:00`, remark: '早餐后2h', createTime: null },
  { id: 215, userId: 1, bloodSugar: 6.5, measureType: 4, measureTime: `${weekDates[5]} 22:00:00`, remark: null, createTime: null },
  { id: 216, userId: 1, bloodSugar: 5.8, measureType: 1, measureTime: `${weekDates[6]} 07:30:00`, remark: null, createTime: null },
  { id: 217, userId: 1, bloodSugar: 7.2, measureType: 3, measureTime: `${weekDates[6]} 09:30:00`, remark: '早餐后2h', createTime: null },
];

// ==================== 饮食记录示例数据 ====================
export const demoDietRecords = [
  { id: 301, userId: 1, foodName: '全麦面包（2片）', mealType: 1, calories: 180, carbs: 35.0, protein: 8.0, fat: 3.0, fiber: 5.0, portion: 100, eatTime: `${weekDates[6]} 07:00:00`, remark: null, createTime: null },
  { id: 302, userId: 1, foodName: '水煮蛋（1个）', mealType: 1, calories: 70, carbs: 1.5, protein: 6.5, fat: 4.5, fiber: 0, portion: 60, eatTime: `${weekDates[6]} 07:15:00`, remark: null, createTime: null },
  { id: 303, userId: 1, foodName: '无糖豆浆（250ml）', mealType: 1, calories: 80, carbs: 8.0, protein: 7.0, fat: 2.5, fiber: 1.0, portion: 250, eatTime: `${weekDates[6]} 07:20:00`, remark: null, createTime: null },
  { id: 304, userId: 1, foodName: '糙米饭（1小碗）', mealType: 2, calories: 200, carbs: 42.0, protein: 4.5, fat: 1.0, fiber: 3.0, portion: 150, eatTime: `${weekDates[6]} 12:00:00`, remark: null, createTime: null },
  { id: 305, userId: 1, foodName: '清蒸鲈鱼', mealType: 2, calories: 150, carbs: 0.5, protein: 28.0, fat: 4.0, fiber: 0, portion: 120, eatTime: `${weekDates[6]} 12:05:00`, remark: null, createTime: null },
  { id: 306, userId: 1, foodName: '蒜蓉西兰花', mealType: 2, calories: 60, carbs: 7.0, protein: 4.0, fat: 1.0, fiber: 3.5, portion: 120, eatTime: `${weekDates[6]} 12:10:00`, remark: null, createTime: null },
  { id: 307, userId: 1, foodName: '荞麦面条', mealType: 3, calories: 220, carbs: 46.0, protein: 8.0, fat: 1.5, fiber: 4.0, portion: 180, eatTime: `${weekDates[6]} 18:00:00`, remark: null, createTime: null },
  { id: 308, userId: 1, foodName: '凉拌黄瓜', mealType: 3, calories: 30, carbs: 5.0, protein: 1.0, fat: 0.5, fiber: 1.5, portion: 100, eatTime: `${weekDates[6]} 18:05:00`, remark: null, createTime: null },
  { id: 309, userId: 1, foodName: '圣女果（10颗）', mealType: 4, calories: 40, carbs: 8.0, protein: 1.5, fat: 0.2, fiber: 2.0, portion: 80, eatTime: `${weekDates[6]} 16:00:00`, remark: '下午加餐', createTime: null },
];

// ==================== 运动记录示例数据 ====================
export const demoExerciseRecords = [
  { id: 401, userId: 1, exerciseTypeId: 1, durationMinutes: 30, caloriesBurned: 90, heartRateAvg: 95, exerciseDate: weekDates[0], remark: null, createTime: null },
  { id: 402, userId: 1, exerciseTypeId: 8, durationMinutes: 45, caloriesBurned: 225, heartRateAvg: 110, exerciseDate: weekDates[1], remark: null, createTime: null },
  { id: 403, userId: 1, exerciseTypeId: 1, durationMinutes: 30, caloriesBurned: 90, heartRateAvg: 93, exerciseDate: weekDates[2], remark: null, createTime: null },
  { id: 404, userId: 1, exerciseTypeId: 5, durationMinutes: 40, caloriesBurned: 233, heartRateAvg: 108, exerciseDate: weekDates[3], remark: '骑自行车通勤', createTime: null },
  { id: 405, userId: 1, exerciseTypeId: 6, durationMinutes: 35, caloriesBurned: 146, heartRateAvg: 88, exerciseDate: weekDates[4], remark: null, createTime: null },
  { id: 406, userId: 1, exerciseTypeId: 8, durationMinutes: 50, caloriesBurned: 250, heartRateAvg: 112, exerciseDate: weekDates[5], remark: null, createTime: null },
  { id: 407, userId: 1, exerciseTypeId: 4, durationMinutes: 30, caloriesBurned: 250, heartRateAvg: 120, exerciseDate: weekDates[6], remark: null, createTime: null },
];

// ==================== 运动类型示例数据（与数据库一致） ====================
export const demoExerciseTypes = [
  { id: 1, typeName: '散步', caloriesPerHour: 180, intensity: 1, suitableFor: '所有糖尿病患者', description: '低强度有氧运动，适合饭后30分钟进行，每次30-45分钟为宜', status: 1, createTime: null },
  { id: 2, typeName: '慢跑', caloriesPerHour: 400, intensity: 2, suitableFor: '血糖控制较好者', description: '中等强度有氧运动，注意监测血糖，避免低血糖', status: 1, createTime: null },
  { id: 3, typeName: '太极拳', caloriesPerHour: 280, intensity: 1, suitableFor: '中老年患者', description: '动作缓慢，有助于改善胰岛素敏感性', status: 1, createTime: null },
  { id: 4, typeName: '游泳', caloriesPerHour: 500, intensity: 2, suitableFor: '无并发症患者', description: '全身性运动，对关节压力小', status: 1, createTime: null },
  { id: 5, typeName: '骑自行车', caloriesPerHour: 350, intensity: 2, suitableFor: '大部分患者', description: '中等强度，注意安全防护', status: 1, createTime: null },
  { id: 6, typeName: '瑜伽', caloriesPerHour: 250, intensity: 1, suitableFor: '血糖稳定者', description: '有助于放松和改善血糖控制', status: 1, createTime: null },
  { id: 7, typeName: '力量训练', caloriesPerHour: 420, intensity: 3, suitableFor: '无严重并发症者', description: '增加肌肉量，提高基础代谢和胰岛素敏感性', status: 1, createTime: null },
  { id: 8, typeName: '快走', caloriesPerHour: 300, intensity: 2, suitableFor: '所有患者', description: '中等强度，简单易行的有氧运动', status: 1, createTime: null },
];

// ==================== 看板示例数据 ====================
export const demoDashboardData = {
  latestWeight: 77.0,
  latestBmi: 25.9,
  latestBodyFat: 27.0,
  latestSystolic: 125,
  latestDiastolic: 81,
  latestHeartRate: 70,
  latestBloodSugar: 5.8,
  todayCalories: 1030,
  todayCarbs: 153,
  todayExerciseCalories: 250,
  bodyTrend: {
    dates: weekDates,
    weightValues: [78.5, 78.2, 78.0, 77.8, 77.5, 77.3, 77.0],
    bmiValues: [26.5, 26.4, 26.3, 26.2, 26.1, 26.0, 25.9],
    bodyFatValues: [28.3, 28.1, 27.9, 27.7, 27.5, 27.3, 27.0],
    systolicValues: [138, 135, 132, 130, 128, 126, 125],
    diastolicValues: [88, 86, 85, 84, 83, 82, 81],
    heartRateValues: [78, 76, 74, 73, 72, 71, 70],
  },
  bloodSugarTrend: {
    dates: ['空腹 ' + weekDates[0], '餐后 ' + weekDates[0], '餐前 ' + weekDates[0], '空腹 ' + weekDates[1], '餐后 ' + weekDates[1], '空腹 ' + weekDates[3], '餐后 ' + weekDates[3], '空腹 ' + weekDates[4], '餐后 ' + weekDates[4], '空腹 ' + weekDates[5], '餐后 ' + weekDates[5], '空腹 ' + weekDates[6], '餐后 ' + weekDates[6]],
    fastingValues: [6.8, 6.5, 6.3, 6.1, 5.9, 5.8],
    beforeMealValues: [5.8, 5.6],
    afterMealValues: [9.2, 8.8, 8.5, 7.9, 7.5, 7.2],
    bedtimeValues: [7.1, 6.8, 6.5],
  },
  dietStats: {
    mealNames: ['全麦面包(早餐)', '水煮蛋(早餐)', '无糖豆浆(早餐)', '糙米饭(午餐)', '清蒸鲈鱼(午餐)', '蒜蓉西兰花(午餐)', '荞麦面条(晚餐)', '凉拌黄瓜(晚餐)', '圣女果(加餐)'],
    calorieValues: [180, 70, 80, 200, 150, 60, 220, 30, 40],
    carbValues: [35, 1.5, 8, 42, 0.5, 7, 46, 5, 8],
    proteinValues: [8, 6.5, 7, 4.5, 28, 4, 8, 1, 1.5],
    fatValues: [3, 4.5, 2.5, 1, 4, 1, 1.5, 0.5, 0.2],
  },
  exerciseStats: {
    dates: weekDates,
    calorieBurnedValues: [90, 225, 90, 233, 146, 250, 250],
    durationValues: [30, 45, 30, 40, 35, 50, 30],
    exerciseTypes: ['1', '8', '1', '5', '6', '8', '4'],
  },
};

// ==================== 健康文章示例数据 ====================
export const demoArticles = [
  {
    id: 1001,
    title: '血糖自我监测的正确方法',
    content: '血糖自我监测是糖尿病管理的重要组成部分。建议空腹血糖控制在4.4-7.0mmol/L，餐后2小时血糖控制在<10.0mmol/L。每天的监测频率应根据治疗方案和血糖控制情况而定，使用胰岛素治疗者建议每天监测3-4次，口服药治疗血糖达标者可每周监测2-4次。\n\n正确的监测步骤：\n1. 用肥皂和温水洗手并擦干\n2. 将试纸条插入血糖仪\n3. 使用采血针刺破指尖侧面\n4. 将血滴接触试纸条\n5. 等待结果显示并记录\n\n监测时机建议：\n- 空腹血糖：反映基础胰岛素分泌功能\n- 餐后2小时血糖：反映餐后血糖控制情况\n- 睡前血糖：指导夜间用药和加餐\n- 凌晨血糖：鉴别空腹高血糖原因',
    summary: '掌握正确的血糖监测频率和方法，是糖尿病管理的第一步。',
    category: 1,
    coverImage: null,
    author: '健康管理师',
    pushStatus: 1,
    viewCount: 1256,
    status: 1,
    createTime: '2026-06-20 09:00:00',
  },
  {
    id: 1002,
    title: '血糖波动的常见原因及应对策略',
    content: '糖尿病患者经常会遇到血糖波动的问题。了解血糖波动的原因有助于更好地管理血糖。\n\n常见原因：\n1. 饮食因素：碳水化合物摄入过多、进餐时间不规律\n2. 运动因素：运动强度过大或过小、运动时间不规律\n3. 药物因素：降糖药物剂量不当、漏服药物\n4. 情绪因素：压力、焦虑、失眠\n5. 疾病因素：感染、发热、外伤\n\n应对策略：\n- 记录饮食和血糖，找出规律\n- 保持规律的运动习惯\n- 按时服药，不随意调整剂量\n- 学会情绪管理，保持良好心态\n- 生病时加强血糖监测',
    summary: '了解引起血糖波动的五大因素，学会科学应对。',
    category: 1,
    coverImage: null,
    author: '内分泌科医生',
    pushStatus: 1,
    viewCount: 980,
    status: 1,
    createTime: '2026-06-22 10:30:00',
  },
  {
    id: 2001,
    title: '糖尿病饮食三大法则：一餐该吃多少',
    content: '控糖饮食并不复杂，用"手掌法则"轻松掌握：主食每餐一个拳头大小（约50-75g生米），蛋白质一个手心大小（约50-100g），蔬菜双手捧起（约300-500g），油脂一个拇指尖大小（约10-15g），水果一个拳头大小（血糖达标时可适量）。\n\n一日三餐能量分配建议：\n- 早餐：25-30%\n- 午餐：35-40%\n- 晚餐：25-30%\n- 加餐：5-10%（根据需要）\n\n推荐的进食顺序：\n先喝汤 → 再吃蔬菜 → 然后吃肉/蛋/豆制品 → 最后吃主食\n这个顺序可以有效延缓血糖上升速度。',
    summary: '用手掌法则轻松控制每餐份量，控糖饮食不再难。',
    category: 2,
    coverImage: null,
    author: '营养师',
    pushStatus: 1,
    viewCount: 1560,
    status: 1,
    createTime: '2026-06-18 14:00:00',
  },
  {
    id: 2002,
    title: '适合糖尿病患者的低GI食物推荐',
    content: 'GI（血糖生成指数）是衡量食物引起血糖升高程度的指标。选择低GI食物有助于平稳血糖。\n\n低GI食物推荐（GI<55）：\n主食类：燕麦、荞麦、糙米、全麦面包、玉米\n蔬菜类：绿叶蔬菜、西兰花、黄瓜、番茄、菌菇类\n水果类：苹果、梨、柚子、樱桃、草莓（适量）\n豆类：黄豆、绿豆、红豆、鹰嘴豆\n蛋白质：鱼、虾、鸡胸肉、鸡蛋、豆腐\n\n避免的高GI食物（GI>70）：\n白米饭、白面包、馒头、糯米、西瓜、荔枝、含糖饮料',
    summary: '选择低GI食物，让血糖更加平稳。',
    category: 2,
    coverImage: null,
    author: '营养师',
    pushStatus: 1,
    viewCount: 1320,
    status: 1,
    createTime: '2026-06-25 08:30:00',
  },
  {
    id: 3001,
    title: '糖尿病常见并发症及早期预警信号',
    content: '糖尿病并发症分为急性和慢性两大类。急性并发症包括低血糖、酮症酸中毒等；慢性并发症累及心血管、肾脏、眼底、神经和足部。出现以下症状需警惕：视力模糊、四肢麻木刺痛感、泡沫尿、足部溃疡不愈合、胸闷气短等。定期体检是预防并发症的关键。\n\n早期预警信号：\n1. 眼睛：视物模糊、眼前有黑影\n2. 肾脏：泡沫尿、眼睑浮肿\n3. 神经：手脚麻木、刺痛、感觉减退\n4. 心血管：胸闷、心悸、活动后气短\n5. 足部：皮肤干燥、皲裂、伤口不易愈合\n\n建议每年进行以下检查：\n- 眼底检查\n- 尿微量白蛋白\n- 血脂全套\n- 神经病变筛查\n- 足部检查',
    summary: '了解并发症早期信号，做到早发现、早干预、早治疗。',
    category: 3,
    coverImage: null,
    author: '内分泌科医生',
    pushStatus: 1,
    viewCount: 2100,
    status: 1,
    createTime: '2026-06-15 16:00:00',
  },
  {
    id: 3002,
    title: '糖尿病足的日常护理指南',
    content: '糖尿病足是糖尿病最严重的并发症之一，但通过正确的日常护理可以有效预防。\n\n每日足部检查要点：\n1. 检查足部皮肤有无破损、水泡、红肿\n2. 观察趾甲有无异常\n3. 检查足部感觉是否正常\n4. 注意足部温度变化\n\n日常护理要点：\n- 每天用温水（不超过37℃）洗脚\n- 洗后彻底擦干，特别是趾缝间\n- 涂抹保湿霜，但趾缝间不要涂抹\n- 趾甲平剪，不要剪得太短\n- 穿棉质袜子，每天更换\n- 鞋子要合脚，新鞋第一天只穿1-2小时\n- 不赤脚走路\n- 避免使用热水袋、电热毯等取暖',
    summary: '每日做好足部护理，远离糖尿病足风险。',
    category: 3,
    coverImage: null,
    author: '糖尿病护理师',
    pushStatus: 1,
    viewCount: 890,
    status: 1,
    createTime: '2026-06-28 11:00:00',
  },
  {
    id: 4001,
    title: '适合糖尿病患者的运动指南',
    content: '运动是糖尿病治疗的"五驾马车"之一。推荐每周至少150分钟中等强度有氧运动，如快走、骑车，每周2-3次抗阻训练。运动前后需监测血糖，血糖<5.6mmol/L应补充碳水，血糖>16.7mmol/L应暂缓运动。最佳运动时间为餐后1小时左右。\n\n运动前注意事项：\n1. 运动前监测血糖\n2. 穿合适的运动鞋和棉袜\n3. 准备一些快速升糖食物（如糖块、果汁）\n4. 避免在胰岛素作用高峰期运动\n5. 运动前充分热身5-10分钟\n\n运动类型推荐：\n- 有氧运动：快走、慢跑、游泳、骑车、太极拳\n- 抗阻运动：弹力带、哑铃、自重训练\n- 柔韧性训练：拉伸、瑜伽\n\n运动禁忌症：\n- 血糖>16.7mmol/L伴酮体\n- 严重视网膜病变\n- 严重心脑血管疾病\n- 严重的糖尿病肾病',
    summary: '科学运动降血糖，这份运动指南请收好。',
    category: 4,
    coverImage: null,
    author: '运动康复师',
    pushStatus: 1,
    viewCount: 780,
    status: 1,
    createTime: '2026-06-24 09:30:00',
  },
  {
    id: 4002,
    title: '饭后散步的好处及正确方式',
    content: '饭后散步是糖尿病患者最简单、最有效的运动方式之一。研究表明，饭后30分钟散步15-20分钟，可以有效降低餐后血糖峰值。\n\n饭后散步的五大好处：\n1. 降低餐后血糖峰值\n2. 促进消化吸收\n3. 改善胰岛素敏感性\n4. 帮助控制体重\n5. 缓解精神压力\n\n正确方法：\n- 时间：饭后休息30分钟后开始\n- 时长：每次20-30分钟\n- 强度：以微出汗、能正常交谈为宜\n- 频率：每天至少1-2次\n- 步速：每分钟100-120步\n\n注意事项：\n- 不要饭后立即剧烈运动\n- 穿着舒适的平底鞋\n- 携带糖尿病识别卡\n- 随身携带含糖食物以防低血糖\n- 天气炎热时选择早晚凉爽时段',
    summary: '饭后百步走，血糖往下走——科学散步降糖法。',
    category: 4,
    coverImage: null,
    author: '运动康复师',
    pushStatus: 1,
    viewCount: 1680,
    status: 1,
    createTime: '2026-06-29 07:00:00',
  },
];
