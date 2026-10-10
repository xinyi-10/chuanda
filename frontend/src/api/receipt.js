import request from './request'

/** 分类导览（含适用场景 / 特点） */
export const listReceiptCategories = () => request.get('/receipt/category/list')

/** 上传凭证（multipart） */
export const uploadReceipt = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/receipt/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 分页列表：{ categoryId, auditStatus, pageNum, pageSize } */
export const pageReceipts = (params) => request.get('/receipt/page', { params })

/** 详情 */
export const getReceipt = (id) => request.get(`/receipt/${id}`)

/** 补录 / 修正时间与金额 */
export const fillReceipt = (id, data) => request.post(`/receipt/${id}/time`, data)

/** 人工纠正分类 */
export const correctReceiptCategory = (id, categoryId) =>
  request.put(`/receipt/${id}/category`, { categoryId })

/** 删除 */
export const deleteReceipt = (id) => request.delete(`/receipt/${id}`)

/**
 * 图片直链（img 标签无法带 Authorization 头，故用 token 查询参数，
 * 后端在 Controller 内手动鉴权并校验归属）。
 */
export const receiptFileUrl = (id) =>
  `/api/receipt/file/${id}?token=${encodeURIComponent(localStorage.getItem('token') || '')}`
