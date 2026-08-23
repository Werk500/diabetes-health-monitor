<template>
  <div class="ai-chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="chat-header">
          <span>AI 糖尿病健康顾问</span>
          <el-tag type="success" size="small">qwen3.7-max</el-tag>
        </div>
      </template>
      <div class="chat-messages" ref="msgContainer">
        <div v-if="messages.length === 0" class="welcome-tip">
          <p>你好！我是你的糖尿病健康管理助手，可以帮你：</p>
          <ul>
            <li>解读血糖数据，提供控糖建议</li>
            <li>推荐适合的饮食方案</li>
            <li>根据你的状况建议运动</li>
            <li>科普并发症预防知识</li>
          </ul>
          <p class="tip-text">试试问我：今天空腹血糖6.5正常吗？</p>
        </div>
        <div v-for="(msg, idx) in messages" :key="idx" :class="['message-item', msg.role === 'user' ? 'user-msg' : 'ai-msg']">
          <div class="msg-avatar">
            <el-avatar :size="36" :icon="msg.role === 'user' ? 'UserFilled' : 'Service'" />
          </div>
          <div class="msg-content">
            <div class="msg-text">{{ msg.content }}</div>
            <div class="msg-time">{{ msg.time }}</div>
          </div>
        </div>
        <div v-if="loading" class="message-item ai-msg">
          <div class="msg-avatar"><el-avatar :size="36" icon="Service" /></div>
          <div class="msg-content">
            <div class="msg-text typing">{{ streamingText || '思考中...' }}<span class="cursor">|</span></div>
          </div>
        </div>
      </div>
      <div class="chat-input">
        <el-input v-model="inputText" type="textarea" :rows="3" placeholder="输入你的问题..." @keydown.enter.exact.prevent="sendMessage" :disabled="loading" />
        <el-button type="primary" @click="sendMessage" :loading="loading" :disabled="!inputText.trim()" style="margin-top:10px;width:100%">
          {{ loading ? 'AI 回复中...' : '发送' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue';
import { aiApi } from '../api/index';
import { ElMessage } from 'element-plus';

const messages = ref([]);
const inputText = ref('');
const loading = ref(false);
const streamingText = ref('');
const msgContainer = ref(null);

const scrollToBottom = () => {
  nextTick(() => {
    if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight;
  });
};

const sendMessage = async () => {
  const text = inputText.value.trim();
  if (!text || loading.value) return;
  const now = new Date().toLocaleTimeString();
  messages.value.push({ role: 'user', content: text, time: now });
  inputText.value = '';
  loading.value = true;
  streamingText.value = '';
  scrollToBottom();

  const token = localStorage.getItem('token');
  try {
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ content: text })
    });

    if (!response.ok) {
      let msg = 'AI 请求失败';
      try {
        const data = await response.json();
        msg = data?.msg || msg;
      } catch (_) { /* ignore JSON parse error */ }
      ElMessage.error(msg);
      messages.value.push({ role: 'assistant', content: '抱歉，请求失败了，请稍后重试。', time: new Date().toLocaleTimeString() });
      return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullText = '';
    let buffer = '';
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      let currentEvent = 'message';
      let dataLines = [];
      const flush = () => {
        const payload = dataLines.join('\n');
        dataLines = [];
        if (currentEvent === 'error') {
          let msg = 'AI 服务暂时不可用，请稍后重试';
          try {
            msg = JSON.parse(payload).msg || msg;
          } catch (_) { /* ignore JSON parse error */ }
          ElMessage.error(msg);
          messages.value.push({ role: 'assistant', content: msg, time: new Date().toLocaleTimeString() });
          streamingText.value = '';
          fullText = '';
        } else {
          fullText += payload;
          streamingText.value = fullText;
          scrollToBottom();
        }
      };
      for (const line of lines) {
        if (line === '') {
          if (dataLines.length > 0) flush();
          currentEvent = 'message';
          continue;
        }
        if (line.startsWith('event:')) {
          currentEvent = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          dataLines.push(line.substring(5).replace(/^ /, ''));
        }
      }
    }
    if (fullText) {
      messages.value.push({ role: 'assistant', content: fullText, time: new Date().toLocaleTimeString() });
      streamingText.value = '';
    }
  } catch (e) {
    ElMessage.error('AI 请求失败');
    messages.value.push({ role: 'assistant', content: '抱歉，请求失败了，请稍后重试。', time: new Date().toLocaleTimeString() });
  } finally {
    loading.value = false;
    scrollToBottom();
  }
};
</script>

<style scoped>
.ai-chat-container { max-width: 800px; margin: 0 auto; }
.chat-card { height: calc(100vh - 140px); display: flex; flex-direction: column; }
.chat-header { display: flex; justify-content: space-between; align-items: center; }
.chat-messages { flex: 1; overflow-y: auto; padding: 10px 0; }
.welcome-tip { text-align: center; color: rgba(148,163,184,0.6); padding: 40px 20px; }
.welcome-tip ul { text-align: left; display: inline-block; margin: 12px 0; line-height: 1.8; }
.tip-text { margin-top: 16px; color: #00d4ff; cursor: pointer; }
.message-item { display: flex; margin-bottom: 16px; padding: 0 10px; }
.user-msg { flex-direction: row-reverse; }
.msg-avatar { margin: 0 10px; }
.msg-content { max-width: 70%; }
.user-msg .msg-content { text-align: right; }
.msg-text { background: rgba(255,255,255,0.06); padding: 10px 14px; border-radius: 8px; line-height: 1.6; white-space: pre-wrap; color: #e2e8f0; }
.user-msg .msg-text { background: linear-gradient(135deg, #00d4ff, #06b6d4); color: #fff; }
.msg-time { font-size: 12px; color: rgba(148,163,184,0.4); margin-top: 4px; }
.typing { background: rgba(255,255,255,0.06); color: #e2e8f0; }
.cursor { animation: blink 1s infinite; }
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }
.chat-input { padding: 10px 0 0; border-top: 1px solid rgba(51,65,85,0.3); }
</style>
