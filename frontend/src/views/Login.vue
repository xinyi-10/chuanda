<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="title">个人生活管理</h2>
      <p class="subtitle">笔记 · 记账 · 智能穿搭</p>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                    show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（可选）" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%"
                   :loading="loading" @click="submit">
          {{ mode === 'login' ? '登 录' : '注册并登录' }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mode = ref('login')
const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '', nickname: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (mode.value === 'login') {
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
    } else {
      await userStore.register(form)
      ElMessage.success('注册成功')
    }
    router.push(route.query.redirect || '/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2a44 0%, #2d3a5c 55%, #24324f 100%);
}
.login-card {
  width: 380px;
  padding: 8px 12px;
}
.title {
  margin: 0 0 4px;
  text-align: center;
  font-size: 22px;
}
.subtitle {
  margin: 0 0 18px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
</style>
