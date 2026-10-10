<template>
  <div class="receipt-page">
    <!-- 分类导览 -->
    <el-card shadow="never" class="guide">
      <template #header>
        <div class="hd">
          <b>凭证分类导览</b>
          <span class="tip">上传支付凭证后，系统会自动识别类型并归类</span>
        </div>
      </template>
      <div class="cat-grid">
        <div v-for="c in categories" :key="c.id" class="cat-card">
          <div class="cat-name">{{ c.name }}</div>
          <div class="cat-scene">{{ c.scene }}</div>
          <div class="cat-feature">{{ c.feature }}</div>
        </div>
      </div>
    </el-card>

    <!-- 上传 -->
    <el-card shadow="never" class="upload-card">
      <template #header><b>上传支付凭证</b></template>
      <el-upload drag :show-file-list="false" :auto-upload="false"
                 accept="image/*" :on-change="onPick">
        <el-icon class="up-icon"><UploadFilled /></el-icon>
        <div class="up-text">将凭证图片拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="up-tip">
            仅支持支付成功截图（需含「支付成功」标识与金额）；同一张图片不可重复上传
          </div>
        </template>
      </el-upload>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="hd">
          <b>我的凭证</b>
          <div class="filters">
            <el-select v-model="query.categoryId" placeholder="全部分类" clearable
                       style="width: 140px" @change="load(1)">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-model="query.auditStatus" placeholder="全部状态" clearable
                       style="width: 140px" @change="load(1)">
              <el-option label="已通过" :value="1" />
              <el-option label="待确认" :value="0" />
            </el-select>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="img-grid">
        <el-empty v-if="!loading && list.length === 0" description="还没有上传凭证" />
        <div v-for="it in list" :key="it.id" class="img-item" :class="{ pending: it.auditStatus === 0 }">
          <el-image :src="receiptFileUrl(it.id)" fit="cover" class="thumb"
                    :preview-src-list="[receiptFileUrl(it.id)]" preview-teleported />
          <div class="info">
            <div class="line1">
              <el-tag size="small" effect="plain">{{ it.categoryName || '未分类' }}</el-tag>
              <el-tag size="small" :type="it.auditStatus === 1 ? 'success' : 'warning'" effect="dark">
                {{ it.auditStatus === 1 ? '已通过' : '待确认' }}
              </el-tag>
            </div>
            <div class="amount" v-if="it.payAmount">¥ {{ formatAmount(it.payAmount) }}</div>
            <div class="time" v-if="it.payTime">{{ formatDateTime(it.payTime) }}</div>
            <div class="ops">
              <el-button v-if="it.auditStatus === 0" link type="primary" @click="openFill(it)">
                补录时间/金额
              </el-button>
              <el-button link @click="openCategory(it)">改分类</el-button>
              <el-button link type="danger" @click="remove(it)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

      <el-pagination v-if="total > 0" class="pager" background
                     layout="prev, pager, next, total"
                     :total="total" :page-size="query.pageSize"
                     :current-page="query.pageNum" @current-change="load" />
    </el-card>

    <!-- 时间补录弹窗（识别不到时间时自动弹出） -->
    <el-dialog v-model="fillVisible" title="补充支付信息" width="460px">
      <el-alert type="warning" :closable="false" show-icon class="mb"
                :title="fillForm.auditMsg || '未能自动识别到完整信息，请手动补充'" />
      <el-form :model="fillForm" label-width="80px">
        <el-form-item label="支付金额" required>
          <el-input-number v-model="fillForm.payAmount" :min="0" :precision="2" style="width: 180px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="支付时间" required>
          <el-date-picker v-model="fillForm.payTime" type="datetime"
                          placeholder="选择支付时间"
                          value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="记账分类" required>
          <el-select v-model="fillForm.ledgerCategoryId" placeholder="请选择（决定支出/收入）" style="width: 100%">
            <el-option-group label="支出">
              <el-option v-for="c in ledgerCategories.filter(x => x.type === 1)"
                         :key="c.id" :label="c.name" :value="c.id" />
            </el-option-group>
            <el-option-group label="收入">
              <el-option v-for="c in ledgerCategories.filter(x => x.type === 2)"
                         :key="c.id" :label="c.name" :value="c.id" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="fillForm.remark" placeholder="可选" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fillVisible = false">稍后再说</el-button>
        <el-button type="primary" :loading="saving" @click="saveFill">确认</el-button>
      </template>
    </el-dialog>

    <!-- 改分类 -->
    <el-dialog v-model="catVisible" title="修正分类" width="380px">
      <el-select v-model="catForm.categoryId" placeholder="选择分类" style="width: 100%">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <template #footer>
        <el-button @click="catVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  listReceiptCategories, uploadReceipt, pageReceipts,
  fillReceipt, correctReceiptCategory, deleteReceipt, receiptFileUrl
} from '@/api/receipt'
import { listCategories as listLedgerCategories } from '@/api/ledger'
import { formatAmount, formatDateTime } from '@/utils/format'

const categories = ref([])
const ledgerCategories = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ categoryId: null, auditStatus: null, pageNum: 1, pageSize: 12 })

const fillVisible = ref(false)
const saving = ref(false)
const fillForm = reactive({
  id: null, payAmount: 0, payTime: '', remark: '', auditMsg: '', ledgerCategoryId: null
})

const catVisible = ref(false)
const catForm = reactive({ id: null, categoryId: null })

async function loadCategories() {
  categories.value = await listReceiptCategories()
  ledgerCategories.value = await listLedgerCategories()
}

async function load(page = 1) {
  loading.value = true
  try {
    query.pageNum = page
    const data = await pageReceipts({
      categoryId: query.categoryId ?? undefined,
      auditStatus: query.auditStatus ?? undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function onPick(file) {
  const raw = file.raw
  if (!raw) return
  try {
    const res = await uploadReceipt(raw)
    if (res.duplicate) {
      ElMessage.warning(res.message)
      return
    }
    ElMessage.success(res.message)
    if (res.similar) {
      ElNotification({ title: '疑似重复', message: res.message, type: 'warning', duration: 6000 })
    }
    await load(1)
    // 未识别到时间 → 自动弹出补录窗
    if (res.needTime) {
      openFill({
        id: res.id,
        payAmount: res.payAmount ? Number(res.payAmount) : 0,
        payTime: res.payTime || '',
        auditMsg: res.auditMsg
      })
    }
  } catch (e) {
    // 拦截器已提示
  }
}

function openFill(it) {
  // 默认选中第一个「支出」类记账分类（凭证多为支出）
  const def = ledgerCategories.value.find((c) => c.type === 1)
  Object.assign(fillForm, {
    id: it.id,
    payAmount: it.payAmount ? Number(it.payAmount) : 0,
    payTime: it.payTime || '',
    remark: it.remark || '',
    auditMsg: it.auditMsg || '',
    ledgerCategoryId: def ? def.id : null
  })
  fillVisible.value = true
}

async function saveFill() {
  if (!fillForm.payAmount || fillForm.payAmount <= 0) {
    ElMessage.warning('请填写正确的支付金额')
    return
  }
  if (!fillForm.payTime) {
    ElMessage.warning('请选择支付时间')
    return
  }
  if (!fillForm.ledgerCategoryId) {
    ElMessage.warning('请选择记账分类（决定这笔记为支出还是收入）')
    return
  }
  saving.value = true
  try {
    await fillReceipt(fillForm.id, {
      payAmount: String(fillForm.payAmount),
      payTime: fillForm.payTime,
      remark: fillForm.remark,
      ledgerCategoryId: fillForm.ledgerCategoryId
    })
    const cat = ledgerCategories.value.find((c) => c.id === fillForm.ledgerCategoryId)
    ElMessage.success(`已确认并自动记入「${cat ? cat.name : '记账'}」`)
    fillVisible.value = false
    await load(query.pageNum)
  } finally {
    saving.value = false
  }
}

function openCategory(it) {
  catForm.id = it.id
  catForm.categoryId = it.categoryId
  catVisible.value = true
}

async function saveCategory() {
  await correctReceiptCategory(catForm.id, catForm.categoryId)
  ElMessage.success('已更新分类')
  catVisible.value = false
  await load(query.pageNum)
}

async function remove(it) {
  try {
    await ElMessageBox.confirm('确定删除该凭证？删除后相同图片可重新上传', '提示', { type: 'warning' })
  } catch { return }
  await deleteReceipt(it.id)
  ElMessage.success('已删除')
  await load(query.pageNum)
}

onMounted(async () => {
  await loadCategories()
  await load(1)
})
</script>

<style scoped>
.receipt-page > * { margin-bottom: 14px; }
.hd { display: flex; align-items: center; justify-content: space-between; }
.tip { color: #909399; font-size: 12px; }
.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 12px;
}
.cat-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 14px;
  background: #fafcff;
}
.cat-name { font-weight: 600; margin-bottom: 6px; }
.cat-scene { font-size: 12px; color: #606266; margin-bottom: 4px; }
.cat-feature { font-size: 12px; color: #a8abb2; }
.up-icon { font-size: 42px; color: #c0c4cc; }
.up-text { color: #606266; font-size: 14px; }
.up-text em { color: #409eff; font-style: normal; }
.up-tip { color: #909399; font-size: 12px; margin-top: 6px; }
.filters { display: flex; gap: 10px; }
.img-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
  min-height: 120px;
}
.img-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.img-item.pending { border-color: #e6a23c; }
.thumb { width: 100%; height: 130px; display: block; background: #f5f7fa; }
.info { padding: 10px 12px; }
.line1 { display: flex; gap: 6px; margin-bottom: 6px; }
.amount { font-size: 17px; font-weight: 700; color: #f56c6c; }
.time { font-size: 12px; color: #909399; margin-top: 2px; }
.ops { margin-top: 6px; display: flex; flex-wrap: wrap; }
.pager { margin-top: 14px; justify-content: flex-end; }
.mb { margin-bottom: 14px; }
.unit { margin-left: 8px; color: #909399; }
</style>
