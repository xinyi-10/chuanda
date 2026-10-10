<template>
  <div class="note-page">
    <!-- 左侧分类 -->
    <el-card class="side" shadow="never">
      <div class="side-hd">
        <span>分类</span>
        <el-button link type="primary" @click="catVisible = true">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
      <ul class="cat-list">
        <li :class="{ active: query.categoryId === null }" @click="selectCategory(null)">
          <span class="cat-name">全部</span>
          <span class="cnt">{{ total }}</span>
        </li>
        <li v-for="c in categories" :key="c.id"
            :class="{ active: query.categoryId === c.id }"
            @click="selectCategory(c.id)">
          <span class="cat-name">{{ c.name }}</span>
          <span class="cnt">{{ c.noteCount }}</span>
          <el-icon class="del" @click.stop="removeCategory(c)"><Close /></el-icon>
        </li>
      </ul>
    </el-card>

    <!-- 右侧主区 -->
    <div class="main">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索标题或内容" clearable
                  style="width: 250px" @keyup.enter="load(1)" @clear="load(1)">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="load(1)">搜索</el-button>
        <div class="spacer" />
        <el-button type="primary" @click="openEdit()">
          <el-icon><Plus /></el-icon>&nbsp;新建笔记
        </el-button>
      </div>

      <div v-loading="loading" class="notes">
        <el-empty v-if="!loading && list.length === 0" description="还没有笔记" />
        <el-card v-for="n in list" :key="n.id" class="note-item" shadow="hover" @click="openEdit(n)">
          <div class="note-hd">
            <span class="note-title">{{ n.title }}</span>
            <el-tag v-if="n.categoryName" size="small" effect="plain">{{ n.categoryName }}</el-tag>
          </div>
          <div class="note-summary">{{ n.content || '（无内容）' }}</div>
          <div class="note-ft">
            <span class="time">{{ formatDateTime(n.updateTime) }}</span>
            <el-button link type="danger" @click.stop="removeNote(n)">删除</el-button>
          </div>
        </el-card>
      </div>

      <el-pagination v-if="total > 0" class="pager" background
                     layout="prev, pager, next, total"
                     :total="total" :page-size="query.pageSize"
                     :current-page="query.pageNum"
                     @current-change="load" />
    </div>

    <!-- 编辑 / 新建笔记 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑笔记' : '新建笔记'" width="640px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="未分类" clearable style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="写点什么…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建分类 -->
    <el-dialog v-model="catVisible" title="新建分类" width="360px">
      <el-input v-model="catName" placeholder="分类名称" maxlength="50"
                @keyup.enter="saveCategory" />
      <template #footer>
        <el-button @click="catVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Close } from '@element-plus/icons-vue'
import {
  pageNotes, getNote, createNote, updateNote, deleteNote,
  listCategories, createCategory, deleteCategory
} from '@/api/note'
import { formatDateTime } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])

const query = reactive({ categoryId: null, keyword: '', pageNum: 1, pageSize: 8 })

const editVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, title: '', content: '', categoryId: null })

const catVisible = ref(false)
const catName = ref('')

async function load(page = 1) {
  loading.value = true
  try {
    query.pageNum = page
    const data = await pageNotes({
      categoryId: query.categoryId,
      keyword: query.keyword || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categories.value = await listCategories()
}

function selectCategory(id) {
  query.categoryId = id
  load(1)
}

function openEdit(note) {
  if (note) {
    Object.assign(form, {
      id: note.id, title: note.title, content: note.content, categoryId: note.categoryId
    })
  } else {
    Object.assign(form, { id: null, title: '', content: '', categoryId: query.categoryId })
  }
  editVisible.value = true
}

async function save() {
  if (!form.title || !form.title.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  saving.value = true
  try {
    const payload = { title: form.title, content: form.content, categoryId: form.categoryId }
    if (form.id) {
      await updateNote(form.id, payload)
      ElMessage.success('保存成功')
    } else {
      await createNote(payload)
      ElMessage.success('创建成功')
    }
    editVisible.value = false
    await Promise.all([load(form.id ? query.pageNum : 1), loadCategories()])
  } finally {
    saving.value = false
  }
}

async function removeNote(n) {
  try {
    await ElMessageBox.confirm(`确定删除笔记「${n.title}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteNote(n.id)
  ElMessage.success('已删除')
  await Promise.all([load(query.pageNum), loadCategories()])
}

async function saveCategory() {
  if (!catName.value.trim()) {
    ElMessage.warning('请输入分类名')
    return
  }
  await createCategory({ name: catName.value.trim(), sort: 0 })
  ElMessage.success('分类已创建')
  catName.value = ''
  catVisible.value = false
  await loadCategories()
}

async function removeCategory(c) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${c.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteCategory(c.id)
  ElMessage.success('已删除')
  if (query.categoryId === c.id) query.categoryId = null
  await Promise.all([load(1), loadCategories()])
}

onMounted(async () => {
  await loadCategories()
  await load(1)
})
</script>

<style scoped>
.note-page {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.side {
  width: 200px;
  flex-shrink: 0;
}
.side-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin-bottom: 8px;
}
.cat-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.cat-list li {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}
.cat-list li:hover {
  background: #f5f7fa;
}
.cat-list li.active {
  background: #ecf5ff;
  color: #409eff;
}
.cat-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cnt {
  font-size: 12px;
  color: #a8abb2;
}
.del {
  opacity: 0;
  color: #f56c6c;
}
.cat-list li:hover .del {
  opacity: 1;
}
.main {
  flex: 1;
  min-width: 0;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.spacer {
  flex: 1;
}
.notes {
  min-height: 200px;
}
.note-item {
  margin-bottom: 12px;
  cursor: pointer;
}
.note-hd {
  display: flex;
  align-items: center;
  gap: 10px;
}
.note-title {
  font-weight: 600;
  font-size: 15px;
}
.note-summary {
  margin: 8px 0;
  color: #909399;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.note-ft {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.time {
  color: #c0c4cc;
  font-size: 12px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
