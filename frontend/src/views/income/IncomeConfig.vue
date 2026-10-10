<template>
  <div class="income-page">
    <el-card shadow="never">
      <template #header>
        <div class="hd">
          <b>固定收入设置</b>
          <el-tag v-if="cfg.initialized" size="small"
                  :type="cfg.remainModify > 0 ? 'success' : 'info'">
            本月剩余修改次数：{{ cfg.remainModify }} / 2
          </el-tag>
        </div>
      </template>

      <el-descriptions v-if="cfg.initialized" :column="2" border>
        <el-descriptions-item label="每月收入金额">
          <span class="money">¥ {{ formatAmount(cfg.monthlyAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="发薪日">每月 {{ cfg.payDay }} 日</el-descriptions-item>
        <el-descriptions-item label="可能延迟">{{ cfg.delayDays }} 天</el-descriptions-item>
        <el-descriptions-item label="预期到账区间">{{ cfg.payDateRange }}</el-descriptions-item>
        <el-descriptions-item label="统计月份">{{ cfg.currentMonth }}</el-descriptions-item>
      </el-descriptions>

      <el-empty v-else description="还没有设置固定收入" />

      <div class="actions">
        <el-button type="primary" :disabled="cfg.initialized && cfg.remainModify <= 0"
                   @click="openEdit">
          {{ cfg.initialized ? '修改设置' : '去设置' }}
        </el-button>
        <el-button v-if="cfg.initialized" @click="postNow">立即补记当月工资</el-button>
        <span v-if="cfg.initialized" class="note">
          规则：每月（自然月）最多修改 2 次，次月 1 日自动重置；首次填写不计次数。
          工资会在发薪日当天由系统自动入账（同月只入一次）。
        </span>
      </div>
    </el-card>

    <!-- 首次引导 / 修改 -->
    <el-dialog v-model="visible" :title="isFirst ? '首次设置固定收入' : '修改固定收入'" width="480px"
               :close-on-click-modal="false" :show-close="!isFirst">
      <el-alert v-if="isFirst" type="info" :closable="false" show-icon class="mb"
                title="请完善以下信息，用于收入统计与到账提醒" />
      <el-alert v-else type="warning" :closable="false" show-icon class="mb"
                :title="`本次修改后将剩余 ${Math.max(cfg.remainModify - 1, 0)} 次修改机会（每月上限 2 次）`" />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="每月收入金额" prop="monthlyAmount">
          <el-input-number v-model="form.monthlyAmount" :min="0" :precision="2" :step="500"
                           style="width: 200px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="每月发薪日" prop="payDay">
          <el-input-number v-model="form.payDay" :min="1" :max="31" />
          <span class="unit">号</span>
        </el-form-item>
        <el-form-item label="可能延迟" prop="delayDays">
          <el-input-number v-model="form.delayDays" :min="0" :max="31" />
          <span class="unit">天</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="!isFirst" @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getIncomeConfig, initIncome, updateIncome, postSalaryNow } from '@/api/income'
import { formatAmount } from '@/utils/format'

const cfg = ref({})
const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ monthlyAmount: 0, payDay: 10, delayDays: 0 })

const isFirst = computed(() => !cfg.value.initialized)

const rules = {
  monthlyAmount: [{ required: true, message: '请填写每月收入金额', trigger: 'blur' }],
  payDay: [{ required: true, message: '请填写发薪日', trigger: 'blur' }]
}

async function load() {
  cfg.value = await getIncomeConfig()
  // 首次进入自动弹出引导
  if (!cfg.value.initialized) {
    openEdit()
  }
}

function openEdit() {
  Object.assign(form, {
    monthlyAmount: cfg.value.monthlyAmount ? Number(cfg.value.monthlyAmount) : 0,
    payDay: cfg.value.payDay || 10,
    delayDays: cfg.value.delayDays || 0
  })
  visible.value = true
}

async function save() {
  await formRef.value.validate()
  if (!form.monthlyAmount || form.monthlyAmount <= 0) {
    ElMessage.warning('每月收入金额必须大于 0')
    return
  }
  saving.value = true
  try {
    const payload = {
      monthlyAmount: String(form.monthlyAmount),
      payDay: form.payDay,
      delayDays: form.delayDays
    }
    if (isFirst.value) {
      cfg.value = await initIncome(payload)
      ElMessage.success('设置成功')
    } else {
      cfg.value = await updateIncome(payload)
      ElMessage.success('修改成功')
    }
    visible.value = false
  } finally {
    saving.value = false
  }
}

async function postNow() {
  await postSalaryNow()
  ElMessage.success('已按当月工资入账（同月不会重复）')
}

onMounted(load)
</script>

<style scoped>
.hd { display: flex; align-items: center; justify-content: space-between; }
.money { color: #f56c6c; font-weight: 700; font-size: 16px; }
.actions { margin-top: 18px; display: flex; align-items: center; gap: 12px; }
.note { color: #909399; font-size: 12px; }
.mb { margin-bottom: 14px; }
.unit { margin-left: 8px; color: #909399; }
</style>
