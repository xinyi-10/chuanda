import request from './request'

/** 待办列表：{ status } */
export const listTodos = (params) => request.get('/todo/list', { params })

/** 详情 */
export const getTodo = (id) => request.get(`/todo/${id}`)

/** 新建 */
export const createTodo = (data) => request.post('/todo', data)

/** 编辑 */
export const updateTodo = (id, data) => request.put(`/todo/${id}`, data)

/** 删除 */
export const deleteTodo = (id) => request.delete(`/todo/${id}`)

/* ===== 倒计时操作 ===== */

export const pauseTodo = (id) => request.post(`/todo/${id}/pause`)
export const resumeTodo = (id) => request.post(`/todo/${id}/resume`)
export const resetTodo = (id) => request.post(`/todo/${id}/reset`)
export const finishTodo = (id) => request.post(`/todo/${id}/finish`)
