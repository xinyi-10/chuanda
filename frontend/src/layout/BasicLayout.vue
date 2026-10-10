<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo">个人生活管理</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon><span>首页</span>
        </el-menu-item>
        <el-menu-item index="/note">
          <el-icon><Notebook /></el-icon><span>笔记</span>
        </el-menu-item>
        <el-menu-item index="/todo">
          <el-icon><AlarmClock /></el-icon><span>待办</span>
        </el-menu-item>
        <el-menu-item index="/ledger">
          <el-icon><Money /></el-icon><span>记账</span>
        </el-menu-item>
        <el-menu-item index="/receipt">
          <el-icon><Tickets /></el-icon><span>支付凭证</span>
        </el-menu-item>
        <el-menu-item index="/income">
          <el-icon><Wallet /></el-icon><span>固定收入</span>
        </el-menu-item>
        <el-menu-item index="/outfit">
          <el-icon><MagicStick /></el-icon><span>智能穿搭</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="hd">
        <span class="title">{{ pageTitle }}</span>
        <div class="right">
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onAvatar">
            <el-tooltip content="点击更换头像" placement="bottom">
              <el-avatar :size="32" :src="avatarSrc" class="avatar">{{ avatarText }}</el-avatar>
            </el-tooltip>
          </el-upload>
          <span class="user">{{ displayName }}</span>
          <el-button text type="primary" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { HomeFilled, Notebook, Money, MagicStick, AlarmClock, Tickets, Wallet } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { uploadAvatar, avatarUrl } from '@/api/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '个人生活管理')
const displayName = computed(() => {
  const u = userStore.userInfo
  return u ? (u.nickname || u.username) : ''
})

const avatarSrc = ref('')
const avatarText = computed(() => (displayName.value || '?').slice(0, 1))

async function onAvatar(file) {
  const raw = file.raw
  if (!raw) return
  try {
    await uploadAvatar(raw)
    avatarSrc.value = avatarUrl()
    ElMessage.success('头像已更新')
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(async () => {
  avatarSrc.value = avatarUrl()
  if (!userStore.userInfo) {
    try {
      await userStore.fetchInfo()
    } catch (e) {
      // 401 已由 axios 拦截器统一处理
    }
  }
})

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid #ebeef5;
}
.menu {
  border-right: none;
}
.hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.title {
  font-size: 15px;
  font-weight: 600;
}
.right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user {
  color: #606266;
  font-size: 14px;
}
.avatar {
  cursor: pointer;
  background: #409eff;
  color: #fff;
  font-size: 14px;
}
.main {
  background: #f5f7fa;
}
</style>
