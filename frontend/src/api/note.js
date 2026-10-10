import request from './request'

/* ===== 笔记 ===== */

/** 分页查询：{ categoryId, keyword, pageNum, pageSize } */
export const pageNotes = (params) => request.get('/note/page', { params })

/** 详情 */
export const getNote = (id) => request.get(`/note/${id}`)

/** 新建 */
export const createNote = (data) => request.post('/note', data)

/** 编辑 */
export const updateNote = (id, data) => request.put(`/note/${id}`, data)

/** 删除（逻辑删） */
export const deleteNote = (id) => request.delete(`/note/${id}`)

/* ===== 分类 ===== */

/** 分类列表（含笔记数） */
export const listCategories = () => request.get('/note/category/list')

/** 新建分类 */
export const createCategory = (data) => request.post('/note/category', data)

/** 重命名 / 改排序 */
export const updateCategory = (id, data) => request.put(`/note/category/${id}`, data)

/** 删除分类（有笔记时后端会拒绝） */
export const deleteCategory = (id) => request.delete(`/note/category/${id}`)
