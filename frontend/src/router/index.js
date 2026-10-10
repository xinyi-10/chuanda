import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  {
    path: '/',
    component: () => import('@/layout/BasicLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      {
        path: 'note',
        name: 'Note',
        component: () => import('@/views/note/NoteList.vue'),
        meta: { title: '笔记', requiresAuth: true }
      },
      {
        path: 'todo',
        name: 'Todo',
        component: () => import('@/views/todo/TodoList.vue'),
        meta: { title: '待办', requiresAuth: true }
      },
      {
        path: 'ledger',
        name: 'Ledger',
        component: () => import('@/views/ledger/LedgerList.vue'),
        meta: { title: '记账', requiresAuth: true }
      },
      {
        path: 'receipt',
        name: 'Receipt',
        component: () => import('@/views/receipt/ReceiptList.vue'),
        meta: { title: '支付凭证', requiresAuth: true }
      },
      {
        path: 'income',
        name: 'Income',
        component: () => import('@/views/income/IncomeConfig.vue'),
        meta: { title: '固定收入', requiresAuth: true }
      },
      {
        path: 'outfit',
        name: 'Outfit',
        component: () => import('@/views/outfit/OutfitPage.vue'),
        meta: { title: '智能穿搭', requiresAuth: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录访问受保护页面 → 跳登录
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    next('/home')
  } else {
    next()
  }
})

export default router
