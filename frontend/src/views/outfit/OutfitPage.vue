<template>
  <div class="outfit-page">
    <!-- 身体数据 -->
    <el-card shadow="never">
      <template #header>
        <div class="hd">
          <b>我的身体数据</b>
          <el-button size="small" type="primary" @click="openBody">编辑</el-button>
        </div>
      </template>

      <template v-if="body">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="身高">{{ body.height }} cm</el-descriptions-item>
          <el-descriptions-item label="体重">{{ body.weight }} kg</el-descriptions-item>
          <el-descriptions-item label="BMI">
            {{ body.bmi }}
            <el-tag size="small" :type="bodyTypeTag(body.bodyType)">{{ body.bodyType }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="三围（胸/腰/臀）">
            {{ body.bust || '-' }} / {{ body.waist || '-' }} / {{ body.hip || '-' }} cm
          </el-descriptions-item>
          <el-descriptions-item label="所在城市">{{ body.city || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="风格偏好">{{ styleLabel(body.stylePref) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="body.bodyTip" class="body-tip">💡 {{ body.bodyTip }}</div>
        <div v-if="body.avatarImageId" class="avatar-line">
          <span class="avatar-label">我的形象：</span>
          <img :src="outfitImageUrl(body.avatarImageId)" class="avatar-thumb" alt="我的形象" />
          <span class="hint">生图时会按这个形象保持一致</span>
        </div>
      </template>

      <el-empty v-else description="还没有填写身体数据 —— 填写后才能获得智能推荐" />
    </el-card>

    <!-- 智能推荐 -->
    <el-card shadow="never">
      <template #header><b>智能穿搭推荐</b></template>
      <div class="rec-bar">
        <el-select v-model="occasion" style="width: 130px">
          <el-option label="日常" value="日常" />
          <el-option label="上班" value="上班" />
          <el-option label="运动" value="运动" />
          <el-option label="聚会" value="聚会" />
        </el-select>
        <el-button type="primary" :loading="recommending" @click="doRecommend">生成推荐</el-button>
        <el-button v-if="suggest" :loading="recommending" @click="doRecommend">换一批</el-button>
        <span class="hint">基于「当地天气 + 体型 + 衣物库」自动搭配</span>
      </div>

      <div v-if="suggest" class="suggest">
        <div class="weather-line">
          <el-tag effect="dark">{{ suggest.weather.city }}</el-tag>
          <span class="temp">{{ suggest.weather.temp }} ℃</span>
          <span class="cond">{{ suggest.weather.condition }}</span>
          <el-tag v-if="!suggest.weather.fromApi" size="small" type="warning" effect="plain">
            天气服务暂不可用，已按季节估算
          </el-tag>
        </div>

        <div class="items-line">
          <el-tag v-for="i in suggest.items" :key="i.id" size="large" class="item-tag" effect="dark">
            {{ i.name }}
          </el-tag>
          <span v-if="!suggest.items.length" class="empty-tip">衣物库为空，先添加几件再看推荐</span>
        </div>

        <div class="advice">{{ suggest.advice }}</div>
        <ul class="reasons">
          <li v-for="(r, i) in suggest.reasons" :key="i">{{ r }}</li>
        </ul>

        <div class="img-gen">
          <el-button type="success" :loading="imgLoading" @click="doGenerateImage">
            {{ imgUrl ? '重新生成效果图' : '生成 3D 效果图' }}
          </el-button>
          <span class="hint">把推荐穿搭穿到 3D 角色身上（约 10–30 秒）</span>
          <div v-if="imgLoading" class="img-loading">
            <el-icon class="is-loading"><Loading /></el-icon> 正在生成，请稍候…
          </div>
          <div v-else-if="imgError" class="img-error">{{ imgError }}</div>
          <div v-else-if="imgUrl" class="img-result">
            <img :src="imgUrl" alt="穿搭效果图" />
            <div class="img-ops">
              <el-button size="small" type="warning" plain @click="doSetAvatar">设为我的形象</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 衣物库 -->
    <el-card shadow="never">
      <template #header>
        <div class="hd">
          <b>我的衣物库（{{ clothes.length }} 件）</b>
          <el-button size="small" type="primary" @click="openCloth()">添加衣物</el-button>
        </div>
      </template>
      <div class="cloth-grid">
        <el-empty v-if="!clothes.length" description="还没有衣物，添加后推荐会更精准" />
        <div v-for="c in clothes" :key="c.id" class="cloth-card">
          <div class="cloth-name">{{ c.name }}</div>
          <div class="cloth-meta">
            <el-tag size="small" effect="plain">{{ labelOf(c.category) }}</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ c.color || '-' }}</el-tag>
            <el-tag size="small" effect="plain" type="success">{{ seasonLabel(c.season) }}</el-tag>
          </div>
          <div class="cloth-ops">
            <el-button link type="primary" @click="openCloth(c)">编辑</el-button>
            <el-button link type="danger" @click="removeCloth(c)">删除</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 历史效果图 -->
    <el-card shadow="never">
      <template #header>
        <div class="hd">
          <b>历史效果图（{{ history.length }}）</b>
          <el-button size="small" text @click="loadHistory">刷新</el-button>
        </div>
      </template>
      <div v-loading="historyLoading" class="img-history">
        <el-empty v-if="!history.length" description="还没有生成过效果图" />
        <div v-for="img in history" :key="img.id" class="hist-card">
          <img v-if="img.status === 1" :src="outfitImageUrl(img.id)" class="hist-img" alt="效果图" />
          <div v-else class="hist-fail">生成失败</div>
          <div class="hist-meta">
            <span class="hist-time">{{ img.createTime }}</span>
            <el-button v-if="img.status === 1" link type="primary" size="small" @click="doSetAvatarById(img.id)">设为形象</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 身体数据弹窗 -->
    <el-dialog v-model="bodyVisible" title="身体数据" width="540px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" show-icon class="mb"
                title="身高体重用于计算 BMI 与版型建议，所在城市用于获取当地天气" />
      <el-form ref="bodyFormRef" :model="bodyForm" :rules="bodyRules" label-width="110px">
        <el-form-item label="性别">
          <el-radio-group v-model="bodyForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="身高(cm)" prop="height">
          <el-input-number v-model="bodyForm.height" :min="100" :max="250" :precision="1" />
        </el-form-item>
        <el-form-item label="体重(kg)" prop="weight">
          <el-input-number v-model="bodyForm.weight" :min="25" :max="200" :precision="1" />
        </el-form-item>
        <el-form-item label="三围(cm)">
          <el-input-number v-model="bodyForm.bust" :min="40" :max="200" :precision="1" placeholder="胸围" />
          <el-input-number v-model="bodyForm.waist" :min="40" :max="200" :precision="1" placeholder="腰围" style="margin-left:8px" />
          <el-input-number v-model="bodyForm.hip" :min="40" :max="200" :precision="1" placeholder="臀围" style="margin-left:8px" />
        </el-form-item>
        <el-form-item label="所在城市" prop="city">
          <el-input v-model="bodyForm.city" placeholder="如 武汉 / Wuhan（用于查天气）" />
        </el-form-item>
        <el-form-item label="风格偏好">
          <el-select v-model="bodyForm.stylePref" placeholder="可选" style="width: 100%">
            <el-option label="休闲" value="casual" />
            <el-option label="正式" value="formal" />
            <el-option label="运动" value="sport" />
            <el-option label="商务" value="business" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bodyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBodyForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 衣物弹窗 -->
    <el-dialog v-model="clothVisible" :title="clothForm.id ? '编辑衣物' : '添加衣物'" width="460px">
      <el-form :model="clothForm" label-width="80px">
        <el-form-item label="衣物图片">
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onClothImage">
            <div class="img-uploader" v-loading="uploading">
              <img v-if="clothForm.imageUrl && clothForm.id" :src="clothingImageUrl(clothForm.id)" class="uploaded-img" />
              <img v-else-if="clothForm.imageUrl" :src="previewUrl" class="uploaded-img" />
              <el-icon v-else class="up-icon-mini"><Plus /></el-icon>
            </div>
          </el-upload>
          <span class="unit">上传你自己衣服的照片，推荐时直接看实物搭配</span>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="clothForm.name" placeholder="如 白色T恤" maxlength="100" />
        </el-form-item>
        <el-form-item label="类别" required>
          <el-select v-model="clothForm.category" style="width: 100%">
            <el-option label="上装" value="top" />
            <el-option label="下装" value="bottom" />
            <el-option label="外套" value="outer" />
            <el-option label="鞋" value="shoes" />
            <el-option label="配饰" value="accessory" />
          </el-select>
        </el-form-item>
        <el-form-item label="主色">
          <el-input v-model="clothForm.color" placeholder="如 白色" maxlength="30" />
        </el-form-item>
        <el-form-item label="适用季节">
          <el-select v-model="clothForm.season" style="width: 100%">
            <el-option label="四季" value="all" />
            <el-option label="春" value="spring" />
            <el-option label="夏" value="summer" />
            <el-option label="秋" value="autumn" />
            <el-option label="冬" value="winter" />
          </el-select>
        </el-form-item>
        <el-form-item label="风格">
          <el-select v-model="clothForm.style" style="width: 100%">
            <el-option label="休闲" value="casual" />
            <el-option label="正式" value="formal" />
            <el-option label="运动" value="sport" />
            <el-option label="商务" value="business" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clothVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClothForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBody, saveBody, listClothing, saveClothing, deleteClothing, recommend,
  uploadClothingImage, recognizeClothing, clothingImageUrl, generateOutfitImage,
  outfitImageStatus, outfitImageUrl, outfitImageList, setAvatar
} from '@/api/outfit'

const body = ref(null)
const clothes = ref([])
const suggest = ref(null)
const occasion = ref('日常')
const recommending = ref(false)

const bodyVisible = ref(false)
const saving = ref(false)
const bodyFormRef = ref()
const bodyForm = reactive({
  gender: 1, height: 170, weight: 60, bust: null, waist: null, hip: null,
  city: '', stylePref: 'casual'
})
const bodyRules = {
  height: [{ required: true, message: '请填写身高', trigger: 'blur' }],
  weight: [{ required: true, message: '请填写体重', trigger: 'blur' }],
  city: [{ required: true, message: '请填写所在城市（用于查天气）', trigger: 'blur' }]
}

const clothVisible = ref(false)
const uploading = ref(false)
const previewUrl = ref('')
const clothForm = reactive({
  id: null, name: '', category: 'top', color: '', season: 'all', style: 'casual', imageUrl: ''
})

/** 衣物图片直链（有图才返回） */
const clothImg = (c) => (c.imageUrl ? clothingImageUrl(c.id) : '')

const CATEGORY_LABEL = { top: '上装', bottom: '下装', outer: '外套', shoes: '鞋', accessory: '配饰' }
const SEASON_LABEL = { all: '四季', spring: '春', summer: '夏', autumn: '秋', winter: '冬' }
const STYLE_LABEL = { casual: '休闲', formal: '正式', sport: '运动', business: '商务' }

const labelOf = (c) => CATEGORY_LABEL[c] || c
const seasonLabel = (s) => SEASON_LABEL[s] || s || '-'
const styleLabel = (s) => STYLE_LABEL[s] || '未设置'
const bodyTypeTag = (t) => (t === '标准' ? 'success' : t === '偏瘦' ? 'warning' : 'danger')

async function loadBody() {
  body.value = await getBody()
  if (!body.value) {
    openBody()   // 首次进入自动引导填写
  }
}

async function loadClothes() {
  clothes.value = await listClothing()
}

function openBody() {
  const b = body.value || {}
  Object.assign(bodyForm, {
    gender: b.gender || 1,
    height: b.height ? Number(b.height) : 170,
    weight: b.weight ? Number(b.weight) : 60,
    bust: b.bust ? Number(b.bust) : null,
    waist: b.waist ? Number(b.waist) : null,
    hip: b.hip ? Number(b.hip) : null,
    city: b.city || '',
    stylePref: b.stylePref || 'casual'
  })
  bodyVisible.value = true
}

async function saveBodyForm() {
  await bodyFormRef.value.validate()
  saving.value = true
  try {
    const payload = {
      gender: bodyForm.gender,
      height: String(bodyForm.height),
      weight: String(bodyForm.weight),
      city: bodyForm.city,
      stylePref: bodyForm.stylePref
    }
    if (bodyForm.bust) payload.bust = String(bodyForm.bust)
    if (bodyForm.waist) payload.waist = String(bodyForm.waist)
    if (bodyForm.hip) payload.hip = String(bodyForm.hip)
    body.value = await saveBody(payload)
    ElMessage.success('已保存身体数据')
    bodyVisible.value = false
  } finally {
    saving.value = false
  }
}

async function doRecommend() {
  if (!body.value) {
    ElMessage.warning('请先填写身体数据')
    openBody()
    return
  }
  recommending.value = true
  try {
    suggest.value = await recommend(occasion.value)
  } finally {
    recommending.value = false
  }
}

function openCloth(c) {
  if (c) {
    Object.assign(clothForm, {
      id: c.id, name: c.name, category: c.category,
      color: c.color || '', season: c.season || 'all', style: c.style || 'casual',
      imageUrl: c.imageUrl || ''
    })
  } else {
    Object.assign(clothForm, {
      id: null, name: '', category: 'top', color: '', season: 'all', style: 'casual', imageUrl: ''
    })
  }
  clothVisible.value = true
}

/** 选择衣物图片 → 立即上传到 RustFS，拿到 key 存进表单；并触发 AI 识别自动填类别/季节/颜色 */
async function onClothImage(file) {
  const raw = file.raw
  if (!raw) return
  uploading.value = true
  try {
    clothForm.imageUrl = await uploadClothingImage(raw)
    previewUrl.value = URL.createObjectURL(raw)   // 未保存前的本地预览
    ElMessage.success('图片已上传')
    await recognizeAndFill(raw)
  } finally {
    uploading.value = false
  }
}

/** AI 识别衣物：成功则自动填充表单，失败提示手动填写 */
async function recognizeAndFill(raw) {
  try {
    const r = await recognizeClothing(raw)
    if (r && r.recognized) {
      if (r.category) clothForm.category = r.category
      if (r.season) clothForm.season = r.season
      if (r.color) clothForm.color = r.color
      if (r.name) clothForm.name = r.name
      ElMessage.success('已自动识别并填充，请确认或修正')
    } else {
      ElMessage.info('未能自动识别，请手动选择类别与季节')
    }
  } catch (e) {
    // 识别失败静默兜底：用户手动填写即可
  }
}

async function saveClothForm() {
  if (!clothForm.name.trim()) {
    ElMessage.warning('请填写衣物名称')
    return
  }
  await saveClothing({ ...clothForm })
  ElMessage.success('已保存')
  clothVisible.value = false
  await loadClothes()
}

async function removeCloth(c) {
  try {
    await ElMessageBox.confirm(`确定删除「${c.name}」？`, '提示', { type: 'warning' })
  } catch { return }
  await deleteClothing(c.id)
  ElMessage.success('已删除')
  await loadClothes()
}

/* ===== 穿搭效果图（AI 生图） ===== */
const imgLoading = ref(false)
const imgError = ref('')
const imgTaskId = ref(null)
const imgUrl = ref('')
let pollTimer = null

async function doGenerateImage() {
  if (!suggest.value || !suggest.value.recordId) {
    ElMessage.warning('请先生成一次穿搭推荐')
    return
  }
  imgLoading.value = true
  imgError.value = ''
  imgUrl.value = ''
  try {
    const taskId = await generateOutfitImage({ outfitRecordId: suggest.value.recordId, occasion: occasion.value })
    imgTaskId.value = taskId
    startPoll()
  } catch (e) {
    imgLoading.value = false
    imgError.value = '提交失败：' + (e?.message || e)
  }
}

function startPoll() {
  clearPoll()
  pollTimer = setInterval(async () => {
    try {
      const st = await outfitImageStatus(imgTaskId.value)
      if (st.status === 1) {
        clearPoll()
        imgLoading.value = false
        imgUrl.value = outfitImageUrl(imgTaskId.value)
        loadHistory()   // 刷新历史效果图
      } else if (st.status === 2 || st.status === 3) {
        clearPoll()
        imgLoading.value = false
        imgError.value = (st.status === 3 ? '生成超时' : '生成失败') + (st.errorMsg ? '：' + st.errorMsg : '')
      }
    } catch (e) {
      clearPoll()
      imgLoading.value = false
      imgError.value = '查询失败：' + (e?.message || e)
    }
  }, 2000)
}

function clearPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function doSetAvatar() {
  if (!imgTaskId.value) {
    ElMessage.warning('请先生成一张效果图')
    return
  }
  await setAvatar(imgTaskId.value)
  ElMessage.success('已设为我的形象，下次生图将保持这个人物')
  body.value = await getBody()   // 刷新身体数据，拿到 avatarImageId
}

/* ===== 历史效果图 ===== */
const history = ref([])
const historyLoading = ref(false)

async function loadHistory() {
  historyLoading.value = true
  try {
    const data = await outfitImageList({ pageNum: 1, pageSize: 20 })
    history.value = data.list || []
  } finally {
    historyLoading.value = false
  }
}

async function doSetAvatarById(id) {
  await setAvatar(id)
  ElMessage.success('已设为我的形象，下次生图将保持这个人物')
  body.value = await getBody()
}

onMounted(async () => {
  await Promise.all([loadBody(), loadClothes(), loadHistory()])
})

onUnmounted(clearPoll)
</script>

<style scoped>
.outfit-page > * { margin-bottom: 14px; }
.hd { display: flex; align-items: center; justify-content: space-between; }
.body-tip {
  margin-top: 12px;
  padding: 10px 14px;
  background: #f0f9eb;
  border-radius: 8px;
  color: #529b2e;
  font-size: 13px;
}
.rec-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.hint { color: #909399; font-size: 12px; }
.suggest {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  background: #fafcff;
}
.weather-line { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.temp { font-size: 22px; font-weight: 700; color: #409eff; }
.cond { color: #606266; }
.items-line { margin-bottom: 12px; }
.item-tag { margin-right: 8px; }
.empty-tip { color: #c0c4cc; font-size: 13px; }
.advice {
  padding: 10px 14px;
  background: #fff;
  border-left: 3px solid #409eff;
  border-radius: 6px;
  margin-bottom: 10px;
  font-size: 14px;
}
.reasons { margin: 0; padding-left: 20px; color: #909399; font-size: 13px; }
.reasons li { margin-bottom: 4px; }
.cloth-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  min-height: 100px;
}
.cloth-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 14px;
}
.cloth-name { font-weight: 600; margin-bottom: 8px; }
.cloth-meta { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 6px; }
.cloth-ops { display: flex; }
.mb { margin-bottom: 14px; }
.img-gen { margin-top: 14px; padding-top: 12px; border-top: 1px dashed #ebeef5; }
.img-gen .hint { margin-left: 10px; }
.img-loading { margin-top: 10px; color: #67c23a; font-size: 13px; display: flex; align-items: center; gap: 6px; }
.img-error { margin-top: 10px; color: #f56c6c; font-size: 13px; }
.img-result { margin-top: 12px; }
.img-result img { max-width: 100%; border-radius: 8px; border: 1px solid #ebeef5; }
.img-ops { margin-top: 8px; }
.avatar-line { margin-top: 12px; display: flex; align-items: center; gap: 8px; }
.avatar-label { color: #606266; font-size: 13px; }
.avatar-thumb { width: 48px; height: 48px; object-fit: cover; border-radius: 8px; border: 1px solid #ebeef5; }
.img-history { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; min-height: 80px; }
.hist-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 8px; }
.hist-img { width: 100%; height: 180px; object-fit: cover; border-radius: 6px; }
.hist-fail { width: 100%; height: 180px; display: flex; align-items: center; justify-content: center; color: #c0c4cc; background: #f5f7fa; border-radius: 6px; font-size: 13px; }
.hist-meta { margin-top: 6px; display: flex; align-items: center; justify-content: space-between; }
.hist-time { color: #909399; font-size: 12px; }
</style>
