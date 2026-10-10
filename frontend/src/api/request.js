import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 实例（统一封装）。
 *
 * 约定：
 *  - 请求前缀 /api，开发期由 Vite 代理转发到后端（见 vite.config.js）
 *  - 自动携带 Authorization: Bearer <token>
 *  - 响应统一处理 { code, msg, data, timestamp }：code===200 直接返回 data，否则提示并 reject
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      // token 失效：清理并回登录页（用 location 跳转，避免与 router 循环依赖）
      localStorage.removeItem('token')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      ElMessage.error(res.msg || '未登录或登录已过期')
      return Promise.reject(res)
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(res)
  },
  (error) => {
    // HTTP 层错误（404/500/超时/断网等）→ 统一转友好中文提示
    let msg = '网络异常，请稍后重试'
    if (error.response) {
      const { status, data } = error.response
      msg = (data && data.msg) ? data.msg : mapStatus(status)
    } else if (error.code === 'ECONNABORTED') {
      msg = '请求超时，请稍后重试'
    } else {
      msg = '网络异常，请检查网络或后端服务是否已启动'
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

/** HTTP 状态码 → 友好中文提示 */
function mapStatus(status) {
  const map = {
    400: '请求参数有误',
    401: '未登录或登录已过期',
    403: '没有权限执行此操作',
    404: '请求的资源不存在',
    405: '请求方式不被支持',
    429: '请求过于频繁，请稍后重试',
    500: '服务器内部错误',
    502: '网关错误，服务暂不可用',
    503: '服务暂时不可用，请稍后重试',
    504: '服务响应超时，请稍后重试'
  }
  return map[status] || `请求失败（${status}）`
}

export default request
