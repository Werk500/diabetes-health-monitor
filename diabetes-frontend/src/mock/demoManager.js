/**
 * 示例数据管理器
 * 通过 localStorage 控制示例数据的开关
 * 生产环境可通过配置关闭
 */
import { ref } from 'vue';

const STORAGE_KEY = 'diabetes_demo_mode_enabled';

// 是否开启示例数据模式（默认为开启）
const demoEnabled = ref(getDemoMode());

function getDemoMode() {
  try {
    const val = localStorage.getItem(STORAGE_KEY);
    // 如果没有设置过，默认开启
    if (val === null) {
      localStorage.setItem(STORAGE_KEY, 'true');
      return true;
    }
    return val === 'true';
  } catch {
    return true;
  }
}

function setDemoMode(enabled) {
  demoEnabled.value = enabled;
  try {
    localStorage.setItem(STORAGE_KEY, String(enabled));
  } catch { /* ignore */ }
}

function toggleDemoMode() {
  setDemoMode(!demoEnabled.value);
  return demoEnabled.value;
}

/**
 * 获取示例数据或真实数据
 * @param {Array|Object} demoData - 示例数据
 * @param {Array|Object} realData - 真实API返回的数据
 * @returns {Array|Object}
 */
function getData(demoData, realData) {
  if (demoEnabled.value) {
    // 如果真实数据为空或不存在，使用示例数据
    if (!realData || (Array.isArray(realData) && realData.length === 0)) {
      return demoData;
    }
    // 如果真实数据存在，合并展示：真实数据在前，示例数据在后
    if (Array.isArray(demoData) && Array.isArray(realData)) {
      return [...realData, ...demoData];
    }
    return realData;
  }
  return realData;
}

/**
 * 判断数据是否为空（不显示示例数据时的空状态判断）
 */
function isEmpty(data) {
  if (!data) return true;
  if (Array.isArray(data)) return data.length === 0;
  if (typeof data === 'object') return Object.keys(data).length === 0;
  return !data;
}

export {
  demoEnabled,
  setDemoMode,
  toggleDemoMode,
  getData,
  isEmpty,
};
