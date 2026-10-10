<template>
  <div>
    <el-alert type="success" :closable="false" show-icon
              title="欢迎使用个人生活管理"
              description="笔记模块已上线；记账、AI 穿搭推荐将在后续阶段落地。" />

    <el-card class="mt" shadow="never">
      <template #header><b>后端连通性</b></template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="应用">{{ health.app || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="health.status === 'up' ? 'success' : 'danger'">
            {{ health.status || 'unknown' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="数据库">
          <el-tag :type="health.dbOk ? 'success' : 'danger'">
            {{ health.dbOk ? (health.database || 'connected') : (health.dbError || 'failed') }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="mt" shadow="never">
      <template #header><b>快捷入口</b></template>
      <el-space>
        <el-button type="primary" @click="$router.push('/note')">去写笔记</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { health as healthApi } from '@/api/auth'

const health = ref({})

onMounted(async () => {
  try {
    health.value = await healthApi()
  } catch (e) {
    // 后端未启动时静默
  }
})
</script>

<style scoped>
.mt {
  margin-top: 16px;
}
</style>
