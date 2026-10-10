<template>
  <div class="ledger-page">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-date-picker v-model="month" type="month" placeholder="选择月份"
                      value-format="YYYY-MM" :clearable="false"
                      @change="loadAll" style="width: 160px" />
      <el-radio-group v-model="filterType" @change="loadRecords">
        <el-radio-button :label="null">全部</el-radio-button>
        <el-radio-button :label="1">支出</el-radio-button>
        <el-radio-button :label="2">收入</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <el-button type="primary" @click="openEdit()">
        <el-icon><Plus /></el-icon>&nbsp;记一笔
      </el-button>
    </div>

    <!-- 概览卡片 -->
    <el-row :gutter="14" class="cards">
      <el-col :span="8">
        <el-card shadow="never" class="stat-card income">
          <div class="label">本月收入</div>
          <div class="value">¥ {{ formatAmount(overview.totalIncome) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card expense">
          <div class="label">本月支出</div>
          <div class="value">¥ {{ formatAmount(overview.totalExpense) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card balance">
          <div class="label">余额（收入 − 支出）</div>
          <div class="value">¥ {{ formatAmount(overview.balance) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="14" class="charts">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><b>支出分类占比</b></template>
          <div ref="pieRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><b>近 6 个月收支趋势</b></template>
          <div ref="barRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 流水表 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-hd">
          <b>流水明细</b>
          <el-select v-model="filterCategoryId" placeholder="全部分类" clearable
                     style="width: 160px" @change="loadRecords">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
      </template>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="recordTime" label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.recordTime) }}</template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.categoryName || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 2 ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.type === 2 ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            <span :class="row.type === 2 ? 'amt-in' : 'amt-out'">
              {{ row.type === 2 ? '+' : '-' }} {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="sourceTag(row.sourceType)" effect="plain">
              {{ sourceText(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="本月暂无流水" /></template>
      </el-table>

      <el-pagination v-if="total > 0" class="pager" background
                     layout="prev, pager, next, total"
                     :total="total" :page-size="query.pageSize"
                     :current-page="query.pageNum" @current-change="loadRecords" />
    </el-card>

    <!-- 记账弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑流水' : '记一笔'" width="520px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type" @change="onTypeChange">
            <el-radio :label="1">支出</el-radio>
            <el-radio :label="2">收入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in typeCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" required>
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="10"
                           style="width: 200px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="form.recordTime" type="datetime"
                          placeholder="默认当前时间"
                          value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="备注（可选）" maxlength="255" />
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
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  pageRecords, createRecord, updateRecord, deleteRecord,
  listCategories, statOverview, statTrend
} from '@/api/ledger'
import { formatAmount, formatDateTime } from '@/utils/format'

const month = ref(new Date().toISOString().slice(0, 7))
const filterType = ref(null)
const filterCategoryId = ref(null)

const records = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])
const overview = ref({})
const trend = ref([])

const query = reactive({ pageNum: 1, pageSize: 8 })

const editVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, type: 1, categoryId: null, amount: 0, recordTime: '', remark: '' })

const pieRef = ref()
const barRef = ref()
let pieChart = null
let barChart = null

const typeCategories = computed(() => categories.value.filter((c) => c.type === form.type))

/* ===== 数据加载 ===== */
function monthRange() {
  const [y, m] = month.value.split('-').map(Number)
  const lastDay = new Date(y, m, 0).getDate()
  return {
    startDate: `${month.value}-01`,
    endDate: `${month.value}-${String(lastDay).padStart(2, '0')}`
  }
}

async function loadRecords(page = 1) {
  loading.value = true
  try {
    query.pageNum = page
    const range = monthRange()
    const data = await pageRecords({
      type: filterType.value ?? undefined,
      categoryId: filterCategoryId.value ?? undefined,
      startDate: range.startDate,
      endDate: range.endDate,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    records.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categories.value = await listCategories()
}

async function loadStats() {
  overview.value = await statOverview({ month: month.value })
  trend.value = await statTrend({ months: 6 })
  await nextTick()
  renderPie()
  renderBar()
}

async function loadAll() {
  filterCategoryId.value = null
  await Promise.all([loadRecords(1), loadCategories(), loadStats()])
}

/* ===== 图表 ===== */
function renderPie() {
  if (!pieRef.value) return
  if (!pieChart) pieChart = echarts.init(pieRef.value)
  const data = (overview.value.byCategory || [])
    .filter((c) => c.type === 1)
    .map((c) => ({ name: c.categoryName, value: Number(c.amount) }))
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '44%'],
      data,
      label: { formatter: '{b}\n{d}%' }
    }]
  })
  pieChart.resize()
}

function renderBar() {
  if (!barRef.value) return
  if (!barChart) barChart = echarts.init(barRef.value)
  const months = trend.value.map((t) => t.month)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'], bottom: 0 },
    grid: { left: 40, right: 16, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'bar', data: trend.value.map((t) => Number(t.income)), itemStyle: { color: '#f56c6c' } },
      { name: '支出', type: 'bar', data: trend.value.map((t) => Number(t.expense)), itemStyle: { color: '#67c23a' } }
    ]
  })
  barChart.resize()
}

/** 流水来源标识：1=手动 2=凭证入账 3=工资自动入账 */
function sourceText(t) {
  return ({ 1: '手动', 2: '凭证', 3: '工资' })[t] || '手动'
}
function sourceTag(t) {
  return ({ 1: 'info', 2: 'warning', 3: 'success' })[t] || 'info'
}

function onResize() {
  pieChart && pieChart.resize()
  barChart && barChart.resize()
}

/* ===== 操作 ===== */
function onTypeChange() {
  form.categoryId = null
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id, type: row.type || 1, categoryId: row.categoryId,
      amount: Number(row.amount), recordTime: row.recordTime || '', remark: row.remark || ''
    })
  } else {
    Object.assign(form, { id: null, type: 1, categoryId: null, amount: 0, recordTime: '', remark: '' })
  }
  editVisible.value = true
}

async function save() {
  if (!form.categoryId) { ElMessage.warning('请选择分类'); return }
  if (!form.amount || form.amount <= 0) { ElMessage.warning('金额必须大于 0'); return }
  const payload = {
    categoryId: form.categoryId,
    amount: String(form.amount),
    recordTime: form.recordTime || null,
    remark: form.remark
  }
  saving.value = true
  try {
    if (form.id) { await updateRecord(form.id, payload); ElMessage.success('保存成功') }
    else { await createRecord(payload); ElMessage.success('记账成功') }
    editVisible.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除这笔「${row.categoryName}」流水？`, '提示', { type: 'warning' })
  } catch { return }
  await deleteRecord(row.id)
  ElMessage.success('已删除')
  await loadAll()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  pieChart && pieChart.dispose()
  barChart && barChart.dispose()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.spacer { flex: 1; }
.cards { margin-bottom: 4px; }
.stat-card .label {
  color: #909399;
  font-size: 13px;
}
.stat-card .value {
  font-size: 24px;
  font-weight: 700;
  margin-top: 6px;
}
.stat-card.income { border-top: 3px solid #f56c6c; }
.stat-card.expense { border-top: 3px solid #67c23a; }
.stat-card.balance { border-top: 3px solid #409eff; }
.stat-card.income .value { color: #f56c6c; }
.stat-card.expense .value { color: #67c23a; }
.stat-card.balance .value { color: #409eff; }
.charts { margin-top: 14px; }
.chart { height: 260px; width: 100%; }
.table-card { margin-top: 14px; }
.card-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pager { margin-top: 12px; justify-content: flex-end; }
.amt-in { color: #f56c6c; font-weight: 600; }
.amt-out { color: #67c23a; font-weight: 600; }
.unit { margin-left: 8px; color: #909399; }
</style>
