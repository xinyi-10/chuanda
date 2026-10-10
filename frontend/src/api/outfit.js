import request from './request'

/* ===== 身体数据 ===== */

export const getBody = () => request.get('/outfit/body')
export const saveBody = (data) => request.post('/outfit/body', data)

/* ===== 衣物库 ===== */

export const listClothing = (params) => request.get('/outfit/clothing', { params })
export const saveClothing = (data) => request.post('/outfit/clothing', data)
export const deleteClothing = (id) => request.delete(`/outfit/clothing/${id}`)

/** 上传衣物图片，返回存储 key（写入 ClothingItem.imageUrl） */
export const uploadClothingImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/outfit/clothing/image', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 识别衣物图片（AI 视觉），返回 { category, season, color, name, recognized } */
export const recognizeClothing = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/outfit/clothing/recognize', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 衣物图片直链（img 标签带不了 Authorization 头，用 token 参数） */
export const clothingImageUrl = (id) =>
  `/api/outfit/clothing/image/${id}?token=${encodeURIComponent(localStorage.getItem('token') || '')}`

/* ===== 推荐 ===== */

export const recommend = (occasion) =>
  request.post('/outfit/recommend', null, { params: { occasion } })
export const outfitHistory = (params) => request.get('/outfit/history', { params })

/* ===== 穿搭效果图（AI 生图） ===== */

/** 触发生成效果图，返回任务 id */
export const generateOutfitImage = (params) => request.post('/outfit/image', null, { params })
/** 查询生成状态 */
export const outfitImageStatus = (id) => request.get(`/outfit/image/status/${id}`)
/** 效果图历史 */
export const outfitImageList = (params) => request.get('/outfit/image/list', { params })
/** 效果图直链（img 标签带不了 Authorization 头，用 token 参数） */
export const outfitImageUrl = (id) =>
  `/api/outfit/image/file/${id}?token=${encodeURIComponent(localStorage.getItem('token') || '')}`

/** 把某张效果图设为「我的形象」（角色参考图，后续生图锁定人物） */
export const setAvatar = (imageId) => request.post('/outfit/image/avatar', null, { params: { imageId } })
