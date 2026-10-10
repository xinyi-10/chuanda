<template>
  <div class="todo-page">
    <div class="toolbar">
      <el-radio-group v-model="filterStatus" @change="load">
        <el-radio-button :label="null">全部</el-radio-button>
        <el-radio-button :label="0">进行中</el-radio-button>
        <el-radio-button :label="1">已暂停</el-radio-button>
        <el-radio-button :label="3">已到期</el-radio-button>
        <el-radio-button :label="2">已完成</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <el-button type="primary" @click="openEdit()">
        <el-icon><Plus /></el-icon>&nbsp;新建待办
      </el-button>
    </div>

    <div v-loading="loading" class="list">
      <el-empty v-if="!loading && list.length === 0" description="暂无待办" />
      <el-card v-for="t in list" :key="t.id" class="todo-item" shadow="hover"
               :class="cardClass(t)">
        <div class="row1">
          <span class="title">{{ t.title }}</span>
          <el-tag v-if="t.priority === 2" type="danger" size="small" effect="dark">紧急</el-tag>
          <el-tag v-else-if="t.priority === 1" type="warning" size="small">重要</el-tag>
          <el-tag :type="statusTagType(t.status)" size="small" effect="plain">{{ statusText(t.status) }}</el-tag>
        </div>

        <div class="countdown" :class="{ expired: isExpired(t), paused: t.status === 1 }">
          <span class="cd-label">{{ cdLabel(t) }}</span>
          <span class="cd-value">{{ fmt(remainOf(t)) }}</span>
        </div>

        <div v-if="t.content" class="content">{{ t.content }}</div>
        <div class="meta">
          <span>截止：{{ formatDateTime(t.deadline) }}</span>
          <span v-if="t.finishTime">· 结束：{{ formatDateTime(t.finishTime) }}</span>
        </div>

        <div class="actions">
          <el-button v-if="t.status === 0" size="small" @click="doPause(t)">暂停</el-button>
          <el-button v-if="t.status === 1" size="small" type="primary" @click="doResume(t)">继续</el-button>
          <el-button v-if="t.status === 0 || t.status === 1 || t.status === 3"
                     size="small" @click="doReset(t)">重置</el-button>
          <el-button v-if="t.status !== 2" size="small" type="success" @click="doFinish(t)">完成</el-button>
          <el-button size="small" @click="openEdit(t)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="doDelete(t)">删除</el-button>
        </div>
      </el-card>
    </div>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑待办' : '新建待办'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="要做的事" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="补充说明（可选）" maxlength="1000" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio :label="0">普通</el-radio>
            <el-radio :label="1">重要</el-radio>
            <el-radio :label="2">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="倒计时">
          <el-radio-group v-model="form.mode">
            <el-radio label="duration">自定义时长</el-radio>
            <el-radio label="deadline">截止时间</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.mode === 'duration'" label="时长">
          <el-input-number v-model="form.durationValue" :min="1" :max="9999" />
          <el-select v-model="form.durationUnit" style="width: 90px; margin-left: 8px">
            <el-option label="秒" value="s" />
            <el-option label="分钟" value="m" />
            <el-option label="小时" value="h" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="截止于">
          <el-date-picker v-model="form.deadlineVal" type="datetime"
                          placeholder="选择截止时间"
                          value-format="YYYY-MM-DDTHH:mm:ss"
                          style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listTodos, createTodo, updateTodo, deleteTodo,
  pauseTodo, resumeTodo, resetTodo, finishTodo
} from '@/api/todo'
import { formatDateTime } from '@/utils/format'

const list = ref([])
const loading = ref(false)
const filterStatus = ref(null)

const editVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: null, title: '', content: '', priority: 0,
  mode: 'duration', durationValue: 25, durationUnit: 'm', deadlineVal: ''
})

/* ===== 实时时钟（每秒刷新，驱动倒计时重算） ===== */
const nowTs = ref(Date.now())
let timer = null
// 已经弹过到期提醒的待办 id，避免重复提醒
const notified = new Set()

function remainOf(t) {
  if (t.status === 1) return Math.max(t.remainSeconds || 0, 0)
  if (t.status === 0 && t.deadline) {
    return Math.max(Math.floor((new Date(t.deadline).getTime() - nowTs.value) / 1000), 0)
  }
  return 0
}

function isExpired(t) {
  return t.status === 3 || (t.status === 0 && remainOf(t) <= 0)
}

function fmt(sec) {
  const s = Math.max(0, Math.floor(sec))
  const h = String(Math.floor(s / 3600)).padStart(2, '0')
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${h}:${m}:${ss}`
}

function cdLabel(t) {
  if (t.status === 2) return '已完成'
  if (t.status === 1) return '已暂停 · 剩余'
  if (t.status === 3) return '已到期'
  return remainOf(t) <= 0 ? '已到期' : '剩余'
}

function statusText(s) {
  return ({ 0: '进行中', 1: '已暂停', 2: '已完成', 3: '已到期' })[s] || '未知'
}

function statusTagType(s) {
  return ({ 0: 'primary', 1: 'info', 2: 'success', 3: 'danger' })[s] || 'info'
}

function cardClass(t) {
  return { 'is-expired': isExpired(t), 'is-done': t.status === 2 }
}

/* ===== 数据加载 ===== */
async function load() {
  loading.value = true
  try {
    list.value = await listTodos({ status: filterStatus.value ?? undefined })
  } finally {
    loading.value = false
  }
}

/* ===== 到期检测：每秒扫描，首次命中即弹提醒 ===== */
function checkExpired() {
  let hit = false
  list.value.forEach((t) => {
    if (t.status === 0 && remainOf(t) <= 0 && !notified.has(t.id)) {
      notified.add(t.id)
      hit = true
      ElNotification({
        title: '⏰ 待办已到期',
        message: t.title,
        type: 'warning',
        duration: 10000,
        position: 'bottom-right'
      })
    }
  })
  // 有条目刚到期，延迟刷新拿后端的最新状态（定时任务会置为「已到期」）
  if (hit) setTimeout(load, 1200)
}

/* ===== 操作 ===== */
async function doPause(t) {
  await pauseTodo(t.id); ElMessage.success('已暂停'); await load()
}
async function doResume(t) {
  await resumeTodo(t.id); ElMessage.success('已继续'); await load()
}
async function doReset(t) {
  await resetTodo(t.id); ElMessage.success('已重置'); notified.delete(t.id); await load()
}
async function doFinish(t) {
  await finishTodo(t.id); ElMessage.success('已完成'); notified.delete(t.id); await load()
}
async function doDelete(t) {
  try {
    await ElMessageBox.confirm(`确定删除待办「${t.title}」？`, '提示', { type: 'warning' })
  } catch { return }
  await deleteTodo(t.id); ElMessage.success('已删除'); notified.delete(t.id); await load()
}

function openEdit(t) {
  if (t) {
    Object.assign(form, {
      id: t.id, title: t.title, content: t.content, priority: t.priority,
      mode: 'duration',
      durationValue: Math.max(1, Math.round((t.duration || 60) / 60)),
      durationUnit: 'm',
      deadlineVal: t.deadline || ''
    })
    // 若原记录无 duration，则切到截止时间模式
    if (!t.duration) form.mode = 'deadline'
  } else {
    Object.assign(form, {
      id: null, title: '', content: '', priority: 0,
      mode: 'duration', durationValue: 25, durationUnit: 'm', deadlineVal: ''
    })
  }
  editVisible.value = true
}

function unitToSec(v, unit) {
  return unit === 's' ? v : unit === 'm' ? v * 60 : v * 3600
}

async function save() {
  if (!form.title.trim()) { ElMessage.warning('请填写标题'); return }
  const payload = { title: form.title, content: form.content, priority: form.priority }
  if (form.mode === 'duration') {
    payload.duration = unitToSec(form.durationValue, form.durationUnit)
  } else {
    if (!form.deadlineVal) { ElMessage.warning('请选择截止时间'); return }
    payload.deadline = form.deadlineVal
  }
  saving.value = true
  try {
    if (form.id) { await updateTodo(form.id, payload); ElMessage.success('保存成功') }
    else { await createTodo(payload); ElMessage.success('创建成功') }
    editVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  timer = setInterval(() => {
    nowTs.value = Date.now()
    checkExpired()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.spacer { flex: 1; }
.list { min-height: 160px; }
.todo-item {
  margin-bottom: 12px;
  border-left: 4px solid #dcdfe6;
  transition: all .2s;
}
.todo-item.is-expired {
  border-left-color: #f56c6c;
  background: #fef0f0;
}
.todo-item.is-done {
  border-left-color: #67c23a;
  opacity: .75;
}
.row1 {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.countdown {
  margin: 10px 0 6px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.cd-label {
  font-size: 12px;
  color: #909399;
}
.cd-value {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 26px;
  font-weight: 700;
  color: #409eff;
  letter-spacing: 1px;
}
.countdown.paused .cd-value { color: #e6a23c; }
.countdown.expired .cd-value { color: #f56c6c; }
.content {
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
}
.meta {
  color: #c0c4cc;
  font-size: 12px;
  margin-bottom: 10px;
  display: flex;
  gap: 10px;
}
.actions { display: flex; gap: 0; }
</style>
