import request from './request'

/** 上传头像 */
export const uploadAvatar = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/auth/avatar', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 头像直链（img 标签无法带 Authorization 头，用 token 参数） */
export const avatarUrl = () =>
  `/api/auth/avatar/file?token=${encodeURIComponent(localStorage.getItem('token') || '')}&t=${Date.now()}`
