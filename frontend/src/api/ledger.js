import request from './request'

/* ===== 流水 ===== */

export const pageRecords = (params) => request.get('/ledger/record/page', { params })
export const getRecord = (id) => request.get(`/ledger/record/${id}`)
export const createRecord = (data) => request.post('/ledger/record', data)
export const updateRecord = (id, data) => request.put(`/ledger/record/${id}`, data)
export const deleteRecord = (id) => request.delete(`/ledger/record/${id}`)

/* ===== 分类 ===== */

export const listCategories = (params) => request.get('/ledger/category/list', { params })
export const createCategory = (data) => request.post('/ledger/category', data)
export const updateCategory = (id, data) => request.put(`/ledger/category/${id}`, data)
export const deleteCategory = (id) => request.delete(`/ledger/category/${id}`)

/* ===== 统计 ===== */

export const statOverview = (params) => request.get('/ledger/stat/overview', { params })
export const statTrend = (params) => request.get('/ledger/stat/trend', { params })
