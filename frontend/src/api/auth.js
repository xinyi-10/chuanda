import request from './request'

/** 登录 */
export const login = (data) => request.post('/auth/login', data)

/** 注册（注册即登录） */
export const register = (data) => request.post('/auth/register', data)

/** 获取当前登录用户信息 */
export const getInfo = () => request.get('/auth/info')

/** 退出登录 */
export const logout = () => request.post('/auth/logout')

/** 健康检查（后端连通性） */
export const health = () => request.get('/health')
